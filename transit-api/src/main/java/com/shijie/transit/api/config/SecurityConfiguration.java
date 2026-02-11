package com.shijie.transit.api.config;

import com.shijie.transit.common.security.JwtAuthenticationFilter;
import com.shijie.transit.common.security.JwtService;
import com.shijie.transit.common.web.ApiErrorWriter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
  @Bean
  @Order(1)
  public SecurityFilterChain userSecurityFilterChain(HttpSecurity http, JwtService jwtService, ApiErrorWriter apiErrorWriter)
      throws Exception {
    JwtAuthenticationFilter jwtFilter =
        new JwtAuthenticationFilter(jwtService, apiErrorWriter, "USER", "/api/user", List.of("/api/user/auth"));

    http.securityMatcher("/api/user/**");
    http.csrf(csrf -> csrf.disable());
    http.cors(cors -> cors.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll());
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, JwtService jwtService, ApiErrorWriter apiErrorWriter)
      throws Exception {
    JwtAuthenticationFilter jwtFilter =
        new JwtAuthenticationFilter(jwtService, apiErrorWriter, "ADMIN", "/api/admin", List.of("/api/admin"));

    http.securityMatcher("/api/admin/**");
    http.csrf(csrf -> csrf.disable());
    http.cors(cors -> cors.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll());
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain otherSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/**");
    http.csrf(csrf -> csrf.disable());
    http.cors(cors -> cors.disable());
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/**").permitAll()
        .requestMatchers("/*.txt").permitAll()
        .anyRequest().denyAll());
    return http.build();
  }
}
