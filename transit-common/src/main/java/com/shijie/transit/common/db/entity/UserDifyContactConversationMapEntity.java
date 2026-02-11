package com.shijie.transit.common.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_dify_contact_conversation_map")
public class UserDifyContactConversationMapEntity extends BaseTenantEntity {
  private Long userId;
  private Long taskId;
  private String wechatContact;
  private String difyConversationId;
  private LocalDateTime lastUsedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public String getWechatContact() {
    return wechatContact;
  }

  public void setWechatContact(String wechatContact) {
    this.wechatContact = wechatContact;
  }

  public String getDifyConversationId() {
    return difyConversationId;
  }

  public void setDifyConversationId(String difyConversationId) {
    this.difyConversationId = difyConversationId;
  }

  public LocalDateTime getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(LocalDateTime lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}

