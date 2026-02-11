package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.UserDifyContactConversationMapEntity;
import com.shijie.transit.common.tenant.TenantContext;
import com.shijie.transit.userapi.mapper.UserDifyContactConversationMapMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DifyContactConversationMappingService {
  private final UserDifyContactConversationMapMapper mapper;
  private final Clock clock;

  public DifyContactConversationMappingService(UserDifyContactConversationMapMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  public String getConversationId(long userId, long taskId, String wechatContact) {
    if (taskId <= 0 || !StringUtils.hasText(wechatContact)) {
      return null;
    }
    UserDifyContactConversationMapEntity existing = mapper.selectOne(
        new LambdaQueryWrapper<UserDifyContactConversationMapEntity>()
            .eq(UserDifyContactConversationMapEntity::getUserId, userId)
            .eq(UserDifyContactConversationMapEntity::getTaskId, taskId)
            .eq(UserDifyContactConversationMapEntity::getWechatContact, wechatContact.trim())
            .orderByDesc(UserDifyContactConversationMapEntity::getLastUsedAt)
            .last("limit 1"));
    return existing == null ? null : existing.getDifyConversationId();
  }

  @Transactional
  public void upsertConversationId(long userId, long taskId, String wechatContact, String conversationId) {
    if (taskId <= 0 || !StringUtils.hasText(wechatContact) || !StringUtils.hasText(conversationId)) {
      return;
    }
    String contact = wechatContact.trim();
    UserDifyContactConversationMapEntity existing = mapper.selectOne(
        new LambdaQueryWrapper<UserDifyContactConversationMapEntity>()
            .eq(UserDifyContactConversationMapEntity::getUserId, userId)
            .eq(UserDifyContactConversationMapEntity::getTaskId, taskId)
            .eq(UserDifyContactConversationMapEntity::getWechatContact, contact)
            .last("limit 1"));
    if (existing == null) {
      UserDifyContactConversationMapEntity entity = new UserDifyContactConversationMapEntity();
      entity.setTenantId(TenantContext.getTenantId());
      entity.setUserId(userId);
      entity.setTaskId(taskId);
      entity.setWechatContact(contact);
      entity.setDifyConversationId(conversationId);
      entity.setLastUsedAt(LocalDateTime.now(clock));
      mapper.insert(entity);
      return;
    }
    existing.setDifyConversationId(conversationId);
    existing.setLastUsedAt(LocalDateTime.now(clock));
    mapper.updateById(existing);
  }
}

