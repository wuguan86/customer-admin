package com.shijie.transit.adminapi.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.adminapi.mapper.AdminUserMapper;
import com.shijie.transit.common.db.entity.AdminUserEntity;
import com.shijie.transit.common.mapper.MembershipPlanMapper;
import com.shijie.transit.common.db.entity.MembershipPlanEntity;
import com.shijie.transit.common.tenant.TenantContext;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
  private final AdminBootstrapProperties properties;
  private final AdminUserMapper adminUserMapper;
  private final MembershipPlanMapper membershipPlanMapper;
  private final PasswordEncoder passwordEncoder;

  public AdminBootstrapRunner(
      AdminBootstrapProperties properties,
      AdminUserMapper adminUserMapper,
      MembershipPlanMapper membershipPlanMapper,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.adminUserMapper = adminUserMapper;
    this.membershipPlanMapper = membershipPlanMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      return;
    }
    TenantContext.setTenantId(properties.getTenantId());
    try {
      ensureAdminUser();
      if (properties.isSeedMembershipPlans()) {
        ensureMembershipPlans();
      }
    } finally {
      TenantContext.clear();
    }
  }

  private void ensureAdminUser() {
    AdminUserEntity existing = adminUserMapper.selectOne(
        new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getUsername, properties.getAdminUsername()));
    if (existing != null) {
      return;
    }
    AdminUserEntity entity = new AdminUserEntity();
    entity.setTenantId(properties.getTenantId());
    entity.setUsername(properties.getAdminUsername());
    entity.setPasswordHash(passwordEncoder.encode(properties.getAdminPassword()));
    entity.setDisplayName(properties.getAdminDisplayName());
    entity.setEnabled(true);
    adminUserMapper.insert(entity);
  }

  /**
   * 初始化会员套餐数据
   * <p>
   * 仅在当前没有任何套餐数据时执行。
   * 会创建三个默认套餐：月付标准版、半年付标准版、企业定制版。
   * 注意：featuresJson 字段手动设置为 JSON 字符串。
   * </p>
   */
  private void ensureMembershipPlans() {
    MembershipPlanEntity monthly = new MembershipPlanEntity();
    monthly.setTenantId(properties.getTenantId());
    monthly.setPlanCode("standard_monthly");
    monthly.setType("SUBSCRIPTION");
    monthly.setName("标准版(单次)");
    monthly.setPriceCents(19900);
    monthly.setDurationDays(30);
    monthly.setSeats(1);
    monthly.setPointsIncluded(3000);
    monthly.setBonusPoints(10);
    monthly.setEnabled(true);
    monthly.setDescription("30 天有效期，适合个人使用");
    monthly.setFeaturesJson("[\"3000 积分\",\"30 天有效期\",\"赠送 10 积分\",\"第一时间获取AI更新信息\"]");

    MembershipPlanEntity halfYear = new MembershipPlanEntity();
    halfYear.setTenantId(properties.getTenantId());
    halfYear.setPlanCode("standard_half_year");
    halfYear.setType("SUBSCRIPTION");
    halfYear.setName("标准版(半年)");
    halfYear.setPriceCents(119900);
    halfYear.setDurationDays(180);
    halfYear.setSeats(1);
    halfYear.setPointsIncluded(18000);
    halfYear.setBonusPoints(500);
    halfYear.setEnabled(true);
    halfYear.setDescription("180 天有效期，性价比更高");
    halfYear.setFeaturesJson("[\"18000 积分\",\"180 天有效期\",\"赠送 500 积分\",\"第一时间获取AI更新信息\"]");

    MembershipPlanEntity enterprise = new MembershipPlanEntity();
    enterprise.setTenantId(properties.getTenantId());
    enterprise.setPlanCode("enterprise");
    enterprise.setType("SUBSCRIPTION");
    enterprise.setName("企业定制及落地服务");
    enterprise.setPriceCents(0);
    enterprise.setDurationDays(365);
    enterprise.setSeats(1);
    enterprise.setPointsIncluded(0);
    enterprise.setBonusPoints(0);
    enterprise.setEnabled(true);
    enterprise.setDescription("企业专属服务，按需沟通报价");
    enterprise.setFeaturesJson("[\"专属顾问沟通\",\"知识库与智能体训练\",\"支持接入企业微信\",\"定制企业微信标签关联\"]");

    MembershipPlanEntity points300 = new MembershipPlanEntity();
    points300.setTenantId(properties.getTenantId());
    points300.setPlanCode("points_300");
    points300.setType("POINTS");
    points300.setName("300积分包");
    points300.setPriceCents(1500);
    points300.setDurationDays(30);
    points300.setSeats(0);
    points300.setPointsIncluded(300);
    points300.setBonusPoints(0);
    points300.setEnabled(true);
    points300.setDescription("立即得300积分，与月包一同刷新");
    points300.setFeaturesJson("[]");

    MembershipPlanEntity points800 = new MembershipPlanEntity();
    points800.setTenantId(properties.getTenantId());
    points800.setPlanCode("points_800");
    points800.setType("POINTS");
    points800.setName("800积分包");
    points800.setPriceCents(4000);
    points800.setDurationDays(30);
    points800.setSeats(0);
    points800.setPointsIncluded(800);
    points800.setBonusPoints(0);
    points800.setEnabled(true);
    points800.setDescription("立即得800积分，与月包一同刷新");
    points800.setFeaturesJson("[]");

    MembershipPlanEntity points1600 = new MembershipPlanEntity();
    points1600.setTenantId(properties.getTenantId());
    points1600.setPlanCode("points_1600");
    points1600.setType("POINTS");
    points1600.setName("1600积分包");
    points1600.setPriceCents(8000);
    points1600.setDurationDays(30);
    points1600.setSeats(0);
    points1600.setPointsIncluded(1600);
    points1600.setBonusPoints(0);
    points1600.setEnabled(true);
    points1600.setDescription("立即得1600积分，与月包一同刷新");
    points1600.setFeaturesJson("[]");

    for (MembershipPlanEntity plan : List.of(monthly, halfYear, enterprise, points300, points800, points1600)) {
      MembershipPlanEntity existing = membershipPlanMapper.selectOne(
          new LambdaQueryWrapper<MembershipPlanEntity>()
              .eq(MembershipPlanEntity::getTenantId, plan.getTenantId())
              .eq(MembershipPlanEntity::getPlanCode, plan.getPlanCode())
      );

      if (existing == null) {
        membershipPlanMapper.insert(plan);
      } else {
        boolean update = false;
        if (existing.getType() == null) {
          existing.setType(plan.getType());
          update = true;
        }
        // Fix legacy featuresJson (starts with "{")
        if (existing.getFeaturesJson() != null && existing.getFeaturesJson().trim().startsWith("{")) {
          existing.setFeaturesJson(plan.getFeaturesJson());
          update = true;
        }

        if (update) {
          membershipPlanMapper.updateById(existing);
        }
      }
    }
  }
}
