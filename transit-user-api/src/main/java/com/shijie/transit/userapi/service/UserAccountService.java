package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.UserAccountEntity;
import com.shijie.transit.common.tenant.TenantContext;
import com.shijie.transit.userapi.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户账户服务
 * <p>
 * 处理C端用户的注册、登录及信息维护。
 * 主要基于微信体系（OpenID/UnionID）进行用户标识。
 * </p>
 */
@Service
public class UserAccountService {
  private final UserAccountMapper userAccountMapper;

  public UserAccountService(UserAccountMapper userAccountMapper) {
    this.userAccountMapper = userAccountMapper;
  }

  /**
   * 基于微信信息更新或创建用户账户 (Upsert)
   * <p>
   * 1. 优先使用 UnionID 查找用户，若无则使用 OpenID。
   * 2. 若用户不存在，则创建新用户，并设置默认昵称/头像（如果为空）。
   * 3. 若用户已存在，则更新其最新的微信信息。
   * </p>
   *
   * @param openId 微信OpenID (必填)
   * @param unionId 微信UnionID (可选)
   * @param nickname 用户昵称 (可选)
   * @param avatarUrl 头像链接 (可选)
   * @return 更新或创建后的用户实体
   */
  @Transactional
  public UserAccountEntity upsertByWeChat(String openId, String unionId, String nickname, String avatarUrl) {
    String safeOpenId = StringUtils.hasText(openId) ? openId.trim() : "";
    String safeUnionId = StringUtils.hasText(unionId) ? unionId.trim() : "";
    String safeNickname = StringUtils.hasText(nickname) ? nickname.trim() : "";
    String safeAvatarUrl = StringUtils.hasText(avatarUrl) ? avatarUrl.trim() : "";

    LambdaQueryWrapper<UserAccountEntity> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(safeUnionId)) {
      wrapper.eq(UserAccountEntity::getWechatUnionId, safeUnionId);
    } else {
      wrapper.eq(UserAccountEntity::getWechatOpenId, safeOpenId);
    }
    UserAccountEntity existing = userAccountMapper.selectOne(wrapper);

    if (existing == null) {
      UserAccountEntity entity = new UserAccountEntity();
      entity.setTenantId(TenantContext.getTenantId());
      entity.setWechatOpenId(safeOpenId);
      entity.setWechatUnionId(safeUnionId);
      entity.setNickname(safeNickname);
      entity.setAvatarUrl(safeAvatarUrl);
      userAccountMapper.insert(entity);
      return entity;
    }

    existing.setWechatOpenId(safeOpenId);
    existing.setWechatUnionId(safeUnionId);
    existing.setNickname(safeNickname);
    existing.setAvatarUrl(safeAvatarUrl);
    userAccountMapper.updateById(existing);
    return existing;
  }

  public UserAccountEntity findById(long id) {
    return userAccountMapper.selectById(id);
  }
}
