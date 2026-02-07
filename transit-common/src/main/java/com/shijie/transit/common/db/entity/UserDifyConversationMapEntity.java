package com.shijie.transit.common.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_dify_conversation_map")
public class UserDifyConversationMapEntity extends BaseTenantEntity {
  private Long userId;
  private String difyConversationId;
  private LocalDateTime lastUsedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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
