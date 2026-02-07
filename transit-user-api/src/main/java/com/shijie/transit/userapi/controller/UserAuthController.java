package com.shijie.transit.userapi.controller;

import com.shijie.transit.userapi.service.WeChatAuthService;
import com.shijie.transit.userapi.wechat.WeChatMpProperties;
import com.shijie.transit.userapi.wechat.WeChatOpenProperties;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/user/auth/wechat")
public class UserAuthController {
  private final WeChatAuthService weChatAuthService;
  private final WeChatMpProperties weChatMpProperties;
  private final WeChatOpenProperties weChatOpenProperties;

  public UserAuthController(
      WeChatAuthService weChatAuthService,
      WeChatMpProperties weChatMpProperties,
      WeChatOpenProperties weChatOpenProperties) {
    this.weChatAuthService = weChatAuthService;
    this.weChatMpProperties = weChatMpProperties;
    this.weChatOpenProperties = weChatOpenProperties;
  }

  @GetMapping("/qrcode")
  public WeChatAuthService.QrCodeResult qrcode(
      @RequestParam(name = "tenantId", required = false, defaultValue = "1") long tenantId,
      @RequestParam(name = "redirect", required = false) String redirect) {
    return weChatAuthService.createQrCode(tenantId, redirect);
  }

  @GetMapping("/callback")
  public ResponseEntity<?> callback(
      @RequestParam(name = "signature", required = false) String signature,
      @RequestParam(name = "msg_signature", required = false) String msgSignature,
      @RequestParam(name = "timestamp", required = false) String timestamp,
      @RequestParam(name = "nonce", required = false) String nonce,
      @RequestParam(name = "echostr", required = false) String echostr,
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "state", required = false) String state) {
    // 强制打印收到的所有参数，看看控制台有没有输出
    System.out.println("======= 微信回调触发 =======");
    System.out.println("code: " + code);
    System.out.println("echostr: " + echostr);

    try {
      if (echostr != null && !echostr.isBlank() && timestamp != null && !timestamp.isBlank() && nonce != null && !nonce.isBlank()) {
        String token = weChatMpProperties.getToken();
        if (token == null || token.isBlank()) {
          return ResponseEntity.status(500)
                  .header("Content-Type", "text/plain; charset=UTF-8")
                  .body("wechat mp token not configured");
        }

        if (msgSignature != null && !msgSignature.isBlank()) {
          if (!verifySignature(token, msgSignature, timestamp, nonce, echostr)) {
            return ResponseEntity.status(403)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body("invalid signature");
          }
          String plain = decryptEchostr(echostr, weChatMpProperties.getEncodingAesKey(), weChatOpenProperties.getAppId());
          if (plain == null) {
            return ResponseEntity.status(403)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body("decrypt failed");
          }
          return ResponseEntity.ok()
                  .header("Content-Type", "text/plain; charset=UTF-8")
                  .body(plain);
        }

        if (signature == null || signature.isBlank()) {
          return ResponseEntity.status(403)
                  .header("Content-Type", "text/plain; charset=UTF-8")
                  .body("signature required");
        }
        if (!verifySignature(token, signature, timestamp, nonce)) {
          return ResponseEntity.status(403)
                  .header("Content-Type", "text/plain; charset=UTF-8")
                  .body("invalid signature");
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(echostr);
      }

      if (code == null || code.isBlank() || state == null || state.isBlank()) {
        return ResponseEntity.badRequest()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body("code/state required");
      }
      WeChatAuthService.CallbackResult result = weChatAuthService.handleCallback(code, state);
      if (result.redirect() == null || result.redirect().isBlank()) {
        String html = """
          <!doctype html>
          <html lang="zh-CN">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>登录成功</title>
            <style>
              body { font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'Noto Sans','PingFang SC','Hiragino Sans GB','Microsoft YaHei',sans-serif; margin: 0; padding: 24px; background: #f6f7fb; color: #111827; }
              .card { max-width: 520px; margin: 0 auto; background: #fff; border: 1px solid rgba(17,24,39,.08); border-radius: 14px; padding: 20px; box-shadow: 0 10px 30px rgba(17,24,39,.08); }
              h3 { margin: 0 0 10px 0; font-size: 18px; }
              p { margin: 8px 0; line-height: 1.6; color: rgba(17,24,39,.72); }
              .tip { margin-top: 14px; font-size: 12px; color: rgba(17,24,39,.55); }
              .btn { display: inline-block; margin-top: 14px; padding: 10px 14px; border-radius: 10px; background: #16a34a; color: #fff; text-decoration: none; }
            </style>
          </head>
          <body>
            <div class="card">
              <h3>微信登录成功</h3>
              <p>请回到电脑端应用，登录会自动完成。</p>
              <p class="tip">你可以直接关闭此页面。</p>
              <a class="btn" href="javascript:window.close()">关闭页面</a>
            </div>
          </body>
          </html>
          """;
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
      }

      String redirectUrl = UriComponentsBuilder.fromUriString(result.redirect())
              .queryParam("token", result.token())
              .queryParam("tenantId", result.tenantId())
              .queryParam("userId", result.userId())
              .build()
              .toUriString();
      return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }catch (Exception e){
      String message = e.getMessage() == null ? "" : e.getMessage();
      if ("login processing".equalsIgnoreCase(message)) {
        String html = buildSimpleHtml("处理中", "已扫码，正在处理登录，请回到电脑端应用。");
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
      }
      if ("login failed".equalsIgnoreCase(message)) {
        String html = buildSimpleHtml("登录失败", "登录失败，请返回客户端刷新二维码后重试。");
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
      }
      if ("state invalid or expired".equalsIgnoreCase(message)) {
        String html = buildSimpleHtml("二维码已过期", "二维码已过期，请返回客户端刷新二维码后重试。");
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
      }
      e.printStackTrace();
      return ResponseEntity.status(500).body(message);
    }
  }

  @GetMapping("/status")
  public WeChatAuthService.LoginPollResult status(@RequestParam("state") String state) {
    return weChatAuthService.pollLogin(state);
  }

  private static boolean verifySignature(String token, String signature, String timestamp, String nonce) {
    String[] parts = new String[] {token, timestamp, nonce};
    Arrays.sort(parts);
    String raw = String.join("", parts);
    String expected = sha1Hex(raw);
    return expected != null && expected.equalsIgnoreCase(signature);
  }

  private static boolean verifySignature(String token, String signature, String timestamp, String nonce, String echostr) {
    String[] parts = new String[] {token, timestamp, nonce, echostr};
    Arrays.sort(parts);
    String raw = String.join("", parts);
    String expected = sha1Hex(raw);
    return expected != null && expected.equalsIgnoreCase(signature);
  }

  private static String sha1Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private static String decryptEchostr(String echostr, String encodingAesKey, String appId) {
    if (encodingAesKey == null || encodingAesKey.isBlank()) {
      return null;
    }
    if (appId == null || appId.isBlank()) {
      return null;
    }
    try {
      byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
      byte[] cipherText = Base64.getDecoder().decode(echostr);
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
      IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
      cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
      byte[] plain = cipher.doFinal(cipherText);

      if (plain.length < 20) {
        return null;
      }

      int msgLen = ((plain[16] & 0xFF) << 24)
          | ((plain[17] & 0xFF) << 16)
          | ((plain[18] & 0xFF) << 8)
          | (plain[19] & 0xFF);
      int msgStart = 20;
      int msgEnd = msgStart + msgLen;
      if (msgLen < 0 || msgEnd > plain.length) {
        return null;
      }

      String msg = new String(Arrays.copyOfRange(plain, msgStart, msgEnd), StandardCharsets.UTF_8);
      String tailAppId = new String(Arrays.copyOfRange(plain, msgEnd, plain.length), StandardCharsets.UTF_8).trim();
      if (!tailAppId.isEmpty() && !tailAppId.equals(appId)) {
        return null;
      }
      return msg;
    } catch (Exception e) {
      return null;
    }
  }

  private static String buildSimpleHtml(String title, String message) {
    return """
      <!doctype html>
      <html lang="zh-CN">
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>%s</title>
        <style>
          body { font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'Noto Sans','PingFang SC','Hiragino Sans GB','Microsoft YaHei',sans-serif; margin: 0; padding: 24px; background: #f6f7fb; color: #111827; }
          .card { max-width: 520px; margin: 0 auto; background: #fff; border: 1px solid rgba(17,24,39,.08); border-radius: 14px; padding: 20px; box-shadow: 0 10px 30px rgba(17,24,39,.08); }
          h3 { margin: 0 0 10px 0; font-size: 18px; }
          p { margin: 8px 0; line-height: 1.6; color: rgba(17,24,39,.72); }
        </style>
      </head>
      <body>
        <div class="card">
          <h3>%s</h3>
          <p>%s</p>
        </div>
      </body>
      </html>
      """.formatted(title, title, message);
  }
}
