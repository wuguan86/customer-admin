package com.shijie.transit.adminapi.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transit.bootstrap")
public class AdminBootstrapProperties {
  private boolean enabled = true;
  private long tenantId = 1;
  private String adminUsername = "admin";
  private String adminPassword = "admin123456";
  private String adminDisplayName = "Administrator";
  private boolean seedMembershipPlans = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getTenantId() {
    return tenantId;
  }

  public void setTenantId(long tenantId) {
    this.tenantId = tenantId;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public void setAdminUsername(String adminUsername) {
    this.adminUsername = adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }

  public String getAdminDisplayName() {
    return adminDisplayName;
  }

  public void setAdminDisplayName(String adminDisplayName) {
    this.adminDisplayName = adminDisplayName;
  }

  public boolean isSeedMembershipPlans() {
    return seedMembershipPlans;
  }

  public void setSeedMembershipPlans(boolean seedMembershipPlans) {
    this.seedMembershipPlans = seedMembershipPlans;
  }
}
