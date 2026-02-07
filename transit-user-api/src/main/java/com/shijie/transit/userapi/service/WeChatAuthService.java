package com.shijie.transit.userapi.service;

import com.shijie.transit.common.security.JwtService;
import com.shijie.transit.common.security.TransitJwtClaims;
import com.shijie.transit.common.tenant.TenantContext;
import com.shijie.transit.common.db.entity.UserAccountEntity;
import com.shijie.transit.userapi.wechat.WeChatAccessTokenResponse;
import com.shijie.transit.userapi.wechat.WeChatClient;
import com.shijie.transit.userapi.wechat.WeChatLoginStateStore;
import com.shijie.transit.userapi.wechat.WeChatOpenProperties;
import com.shijie.transit.userapi.wechat.WeChatUserInfoResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WeChatAuthService {
  private final WeChatOpenProperties properties;
  private final WeChatClient weChatClient;
  private final WeChatLoginStateStore stateStore;
  private final UserAccountService userAccountService;
  private final JwtService jwtService;

  public WeChatAuthService(
      WeChatOpenProperties properties,
      WeChatClient weChatClient,
      WeChatLoginStateStore stateStore,
      UserAccountService userAccountService,
      JwtService jwtService) {
    this.properties = properties;
    this.weChatClient = weChatClient;
    this.stateStore = stateStore;
    this.userAccountService = userAccountService;
    this.jwtService = jwtService;
  }

  public QrCodeResult createQrCode(long tenantId, String redirect) {
    String state = stateStore.createState(tenantId, redirect);
    String callbackUrlEncoded = URLEncoder.encode(properties.getCallbackUrl(), StandardCharsets.UTF_8);
    String url = "https://open.weixin.qq.com/connect/oauth2/authorize"
        + "?appid=" + properties.getAppId()
        + "&redirect_uri=" + callbackUrlEncoded
        + "&response_type=code"
        + "&scope=snsapi_userinfo"
        + "&state=" + state
        + "#wechat_redirect";
    return new QrCodeResult(url, state);
  }

  public CallbackResult handleCallback(String code, String state) {
    if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
      throw new IllegalArgumentException("code/state required");
    }

    WeChatLoginStateStore.Snapshot snapshot = stateStore.snapshot(state);
    if (snapshot.status() == WeChatLoginStateStore.Status.COMPLETED
        && snapshot.callbackValue() != null
        && snapshot.stateValue() != null) {
      return new CallbackResult(
          snapshot.callbackValue().token(),
          snapshot.stateValue().redirect(),
          snapshot.callbackValue().userId(),
          snapshot.callbackValue().tenantId());
    }
    if (snapshot.status() == WeChatLoginStateStore.Status.PROCESSING) {
      throw new IllegalStateException("login processing");
    }
    if (snapshot.status() == WeChatLoginStateStore.Status.FAILED) {
      throw new IllegalStateException("login failed");
    }
    if (snapshot.status() == WeChatLoginStateStore.Status.EXPIRED
        || snapshot.status() == WeChatLoginStateStore.Status.INVALID) {
      throw new IllegalArgumentException("state invalid or expired");
    }

    WeChatLoginStateStore.StateValue stateValue = stateStore.beginCallback(state);
    if (stateValue == null) {
      throw new IllegalArgumentException("state invalid or expired");
    }

    TenantContext.setTenantId(stateValue.tenantId());
    try {
      WeChatAccessTokenResponse tokenResp =
          weChatClient.exchangeCodeForToken(properties.getAppId(), properties.getAppSecret(), code);
      if (tokenResp == null) {
          throw new IllegalStateException("wechat token exchange returned null response");
      }
      if (tokenResp.getErrCode() != null && tokenResp.getErrCode() != 0) {
        throw new IllegalStateException("wechat token exchange failed: errcode=" + tokenResp.getErrCode() + ", errmsg=" + tokenResp.getErrMsg());
      }
      if (!StringUtils.hasText(tokenResp.getAccessToken()) || !StringUtils.hasText(tokenResp.getOpenId())) {
        throw new IllegalStateException("wechat token exchange missing fields");
      }

      WeChatUserInfoResponse userInfo =
          weChatClient.fetchUserInfo(tokenResp.getAccessToken(), tokenResp.getOpenId());
      if (userInfo == null) {
          throw new IllegalStateException("wechat userinfo returned null response");
      }
      if (userInfo.getErrCode() != null && userInfo.getErrCode() != 0) {
        throw new IllegalStateException("wechat userinfo failed: errcode=" + userInfo.getErrCode() + ", errmsg=" + userInfo.getErrMsg());
      }

      String nickname = userInfo.getNickname();
      String avatarUrl = userInfo.getHeadImgUrl();
      String openId = tokenResp.getOpenId();
      String unionId = StringUtils.hasText(userInfo.getUnionId()) ? userInfo.getUnionId() : tokenResp.getUnionId();

      UserAccountEntity user = userAccountService.upsertByWeChat(openId, unionId, nickname, avatarUrl);
      String jwt = jwtService.issueToken(new TransitJwtClaims(user.getId(), stateValue.tenantId(), "USER"));
      CallbackResult result = new CallbackResult(jwt, stateValue.redirect(), user.getId(), stateValue.tenantId());
      stateStore.complete(state, new WeChatLoginStateStore.CallbackValue(result.token(), result.userId(), result.tenantId()));
      return result;
    } catch (RuntimeException e) {
      stateStore.fail(state);
      throw e;
    } finally {
      TenantContext.clear();
    }
  }

  public LoginPollResult pollLogin(String state) {
    WeChatLoginStateStore.PollResult poll = stateStore.poll(state);
    if (poll.callbackValue() == null) {
      return new LoginPollResult(poll.status().name(), null, null, null);
    }
    return new LoginPollResult(
        poll.status().name(),
        poll.callbackValue().token(),
        poll.callbackValue().userId(),
        poll.callbackValue().tenantId());
  }

  public record QrCodeResult(String url, String state) {
  }

  public record CallbackResult(String token, String redirect, long userId, long tenantId) {
  }

  public record LoginPollResult(String status, String token, Long userId, Long tenantId) {
  }
}
