package com.raisetech.tripdiary.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.raisetech.tripdiary.mapper")
public class MyBatisConfig {}
