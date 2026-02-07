package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.MembershipPlanEntity;
import com.shijie.transit.common.db.entity.PointsLedgerEntity;
import com.shijie.transit.common.db.entity.UserMembershipEntity;
import com.shijie.transit.common.mapper.MembershipPlanMapper;
import com.shijie.transit.userapi.mapper.PointsLedgerMapper;
import com.shijie.transit.userapi.mapper.UserMembershipMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipQueryService {
  private final MembershipPlanMapper membershipPlanMapper;
  private final UserMembershipMapper userMembershipMapper;
  private final PointsLedgerMapper pointsLedgerMapper;

  public MembershipQueryService(
      MembershipPlanMapper membershipPlanMapper,
      UserMembershipMapper userMembershipMapper,
      PointsLedgerMapper pointsLedgerMapper) {
    this.membershipPlanMapper = membershipPlanMapper;
    this.userMembershipMapper = userMembershipMapper;
    this.pointsLedgerMapper = pointsLedgerMapper;
  }

  public List<MembershipPlanEntity> listEnabledPlans() {
    return membershipPlanMapper.selectList(
        new LambdaQueryWrapper<MembershipPlanEntity>()
            .eq(MembershipPlanEntity::getEnabled, true)
            .orderByAsc(MembershipPlanEntity::getId));
  }

  public UserMembershipEntity findActiveMembership(long userId) {
    return userMembershipMapper.selectOne(
        new LambdaQueryWrapper<UserMembershipEntity>()
            .eq(UserMembershipEntity::getUserId, userId)
            .orderByDesc(UserMembershipEntity::getEndAt)
            .last("limit 1"));
  }

  public List<PointsLedgerEntity> listPointsLedger(long userId, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return pointsLedgerMapper.selectList(
        new LambdaQueryWrapper<PointsLedgerEntity>()
            .eq(PointsLedgerEntity::getUserId, userId)
            .orderByDesc(PointsLedgerEntity::getCreatedAt)
            .last("limit " + safeLimit));
  }
}
