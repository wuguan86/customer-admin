package com.shijie.transit.userapi.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.shiliu.transit.userapi.mapper")
public class UserMybatisConfiguration {
}
