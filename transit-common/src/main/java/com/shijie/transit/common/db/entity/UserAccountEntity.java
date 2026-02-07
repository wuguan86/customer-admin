package com.shijie.transit.common.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user_account")
public class UserAccountEntity extends BaseTenantEntity {
  private String wechatOpenId;
  private String wechatUnionId;
  private String nickname;
  private String avatarUrl;

  public String getWechatOpenId() {
    return wechatOpenId;
  }

  public void setWechatOpenId(String wechatOpenId) {
    this.wechatOpenId = wechatOpenId;
  }

  public String getWechatUnionId() {
    return wechatUnionId;
  }

  public void setWechatUnionId(String wechatUnionId) {
    this.wechatUnionId = wechatUnionId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }
}
