package com.shijie.transit.userapi.controller;

import com.shijie.transit.common.db.entity.UserAccountEntity;
import com.shijie.transit.common.security.TransitPrincipal;
import com.shijie.transit.userapi.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserMeController {
  private final UserAccountService userAccountService;

  public UserMeController(UserAccountService userAccountService) {
    this.userAccountService = userAccountService;
  }

  @GetMapping("/me")
  public UserMeResponse me() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    TransitPrincipal principal = (TransitPrincipal) authentication.getPrincipal();
    UserAccountEntity user = userAccountService.findById(principal.subjectId());
    return new UserMeResponse(user.getId(), user.getTenantId(), user.getNickname(), user.getAvatarUrl());
  }

  public record UserMeResponse(long id, long tenantId, String nickname, String avatarUrl) {
  }
}
