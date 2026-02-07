package com.shijie.transit.adminapi.controller;

import com.shijie.transit.adminapi.service.PaymentConfigService;
import com.shijie.transit.common.db.entity.PaymentConfigEntity;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payment/config")
public class AdminPaymentConfigController {
  private final PaymentConfigService paymentConfigService;

  public AdminPaymentConfigController(PaymentConfigService paymentConfigService) {
    this.paymentConfigService = paymentConfigService;
  }

  @GetMapping
  public List<PaymentConfigEntity> list() {
    return paymentConfigService.listAll();
  }

  @GetMapping("/{method}")
  public PaymentConfigEntity get(@PathVariable("method") String method) {
    return paymentConfigService.getByMethod(method.toUpperCase());
  }

  @PutMapping("/{method}")
  public PaymentConfigEntity save(@PathVariable("method") String method, @RequestBody SaveRequest request) {
    return paymentConfigService.upsert(method, request.enabled(), request.configJson());
  }

  public record SaveRequest(Boolean enabled, @NotBlank String configJson) {
  }
}
