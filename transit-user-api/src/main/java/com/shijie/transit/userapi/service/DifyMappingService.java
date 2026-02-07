package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.UserDifyConversationMapEntity;
import com.shijie.transit.common.db.entity.UserDifyKnowledgeBaseMapEntity;
import com.shijie.transit.common.tenant.TenantContext;
import com.shijie.transit.userapi.mapper.UserDifyConversationMapMapper;
import com.shijie.transit.userapi.mapper.UserDifyKnowledgeBaseMapMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DifyMappingService {
  private final UserDifyKnowledgeBaseMapMapper kbMapMapper;
  private final UserDifyConversationMapMapper conversationMapMapper;
  private final Clock clock;

  public DifyMappingService(
      UserDifyKnowledgeBaseMapMapper kbMapMapper,
      UserDifyConversationMapMapper conversationMapMapper,
      Clock clock) {
    this.kbMapMapper = kbMapMapper;
    this.conversationMapMapper = conversationMapMapper;
    this.clock = clock;
  }

  @Transactional
  public void bindKnowledgeBase(long userId, String knowledgeBaseId) {
    if (!StringUtils.hasText(knowledgeBaseId)) {
      throw new IllegalArgumentException("knowledgeBaseId required");
    }
    UserDifyKnowledgeBaseMapEntity existing = kbMapMapper.selectOne(
        new LambdaQueryWrapper<UserDifyKnowledgeBaseMapEntity>().eq(UserDifyKnowledgeBaseMapEntity::getUserId, userId));
    if (existing == null) {
      UserDifyKnowledgeBaseMapEntity entity = new UserDifyKnowledgeBaseMapEntity();
      entity.setTenantId(TenantContext.getTenantId());
      entity.setUserId(userId);
      entity.setDifyKnowledgeBaseId(knowledgeBaseId.trim());
      kbMapMapper.insert(entity);
      return;
    }
    existing.setDifyKnowledgeBaseId(knowledgeBaseId.trim());
    kbMapMapper.updateById(existing);
  }

  public String getBoundKnowledgeBase(long userId) {
    UserDifyKnowledgeBaseMapEntity existing = kbMapMapper.selectOne(
        new LambdaQueryWrapper<UserDifyKnowledgeBaseMapEntity>().eq(UserDifyKnowledgeBaseMapEntity::getUserId, userId));
    return existing == null ? null : existing.getDifyKnowledgeBaseId();
  }

  @Transactional
  public void recordConversation(long userId, String conversationId) {
    if (!StringUtils.hasText(conversationId)) {
      return;
    }
    UserDifyConversationMapEntity existing = conversationMapMapper.selectOne(
        new LambdaQueryWrapper<UserDifyConversationMapEntity>()
            .eq(UserDifyConversationMapEntity::getUserId, userId)
            .eq(UserDifyConversationMapEntity::getDifyConversationId, conversationId));
    if (existing == null) {
      UserDifyConversationMapEntity entity = new UserDifyConversationMapEntity();
      entity.setTenantId(TenantContext.getTenantId());
      entity.setUserId(userId);
      entity.setDifyConversationId(conversationId);
      entity.setLastUsedAt(LocalDateTime.now(clock));
      conversationMapMapper.insert(entity);
      return;
    }
    existing.setLastUsedAt(LocalDateTime.now(clock));
    conversationMapMapper.updateById(existing);
  }

  public String getLatestConversationId(long userId) {
    UserDifyConversationMapEntity existing = conversationMapMapper.selectOne(
        new LambdaQueryWrapper<UserDifyConversationMapEntity>()
            .eq(UserDifyConversationMapEntity::getUserId, userId)
            .orderByDesc(UserDifyConversationMapEntity::getLastUsedAt)
            .last("limit 1"));
    return existing == null ? null : existing.getDifyConversationId();
  }
}
