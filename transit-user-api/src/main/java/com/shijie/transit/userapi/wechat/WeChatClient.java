package com.shijie.transit.userapi.wechat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WeChatClient {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public WeChatClient(ObjectMapper objectMapper) {
    this.restClient = RestClient.create();
    this.objectMapper = objectMapper;
  }

  public WeChatAccessTokenResponse exchangeCodeForToken(String appId, String appSecret, String code) {
    byte[] responseBody = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host("api.weixin.qq.com")
            .path("/sns/oauth2/access_token")
            .queryParam("appid", appId)
            .queryParam("secret", appSecret)
            .queryParam("code", code)
            .queryParam("grant_type", "authorization_code")
            .build())
        .retrieve()
        .body(byte[].class);

    try {
      String response = responseBody == null ? "" : new String(responseBody, StandardCharsets.UTF_8);
      return objectMapper.readValue(response, WeChatAccessTokenResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse WeChat access token response", e);
    }
  }

  public WeChatUserInfoResponse fetchUserInfo(String accessToken, String openId) {
    byte[] responseBody = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host("api.weixin.qq.com")
            .path("/sns/userinfo")
            .queryParam("access_token", accessToken)
            .queryParam("openid", openId)
            .build())
        .retrieve()
        .body(byte[].class);

    try {
      String response = responseBody == null ? "" : new String(responseBody, StandardCharsets.UTF_8);
      return objectMapper.readValue(response, WeChatUserInfoResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse WeChat user info response", e);
    }
  }
}
