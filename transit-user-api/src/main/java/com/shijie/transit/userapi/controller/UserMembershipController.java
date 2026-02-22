package com.shijie.transit.userapi.controller;

import com.shijie.transit.common.db.entity.MembershipPlanEntity;
import com.shijie.transit.common.db.entity.PointsLedgerEntity;
import com.shijie.transit.common.db.entity.UserMembershipEntity;
import com.shijie.transit.common.mapper.MembershipPlanMapper;
import com.shijie.transit.common.security.TransitPrincipal;
import com.shijie.transit.common.web.Result;
import com.shijie.transit.userapi.service.MembershipQueryService;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserMembershipController {
  private final MembershipQueryService membershipQueryService;
  private final MembershipPlanMapper membershipPlanMapper;

  public UserMembershipController(MembershipQueryService membershipQueryService, MembershipPlanMapper membershipPlanMapper) {
    this.membershipQueryService = membershipQueryService;
    this.membershipPlanMapper = membershipPlanMapper;
  }

  @GetMapping("/membership/plans")
  public Result<List<MembershipPlanEntity>> plans() {
    return Result.success(membershipQueryService.listEnabledPlans());
  }

  @GetMapping("/membership/me")
  public Result<MyMembershipResponse> myMembership() {
    TransitPrincipal principal = currentPrincipal();
    UserMembershipEntity membership = membershipQueryService.findActiveMembership(principal.subjectId());
    if (membership == null) {
      return Result.success(new MyMembershipResponse(null, null));
    }
    MembershipPlanEntity plan = membership.getPlanId() == null ? null : membershipPlanMapper.selectById(membership.getPlanId());
    return Result.success(new MyMembershipResponse(
        new MembershipInfo(membership.getStatus(), membership.getStartAt(), membership.getEndAt(), membership.getPointsBalance()),
        plan));
  }

  @GetMapping("/points/ledger")
  public Result<List<PointsLedgerEntity>> pointsLedger(@RequestParam(name = "limit", required = false, defaultValue = "50") int limit) {
    TransitPrincipal principal = currentPrincipal();
    return Result.success(membershipQueryService.listPointsLedger(principal.subjectId(), limit));
  }

  private TransitPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (TransitPrincipal) authentication.getPrincipal();
  }

  public record MyMembershipResponse(MembershipInfo membership, MembershipPlanEntity plan) {
  }

  public record MembershipInfo(String status, LocalDateTime startAt, LocalDateTime endAt, Integer pointsBalance) {
  }
}
