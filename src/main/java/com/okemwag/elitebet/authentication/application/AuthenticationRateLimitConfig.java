package com.okemwag.elitebet.authentication.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthenticationRateLimitProperties.class)
public class AuthenticationRateLimitConfig {
}
