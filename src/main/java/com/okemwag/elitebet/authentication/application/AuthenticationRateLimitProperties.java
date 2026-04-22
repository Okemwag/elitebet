package com.okemwag.elitebet.authentication.application;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "elitebet.auth.rate-limit")
public record AuthenticationRateLimitProperties(
		@Min(1) long registrationIpMaxAttempts,
		@Min(1) long registrationIdentityMaxAttempts,
		@NotNull Duration registrationWindow) {

	public AuthenticationRateLimitProperties {
		if (registrationIpMaxAttempts == 0) {
			registrationIpMaxAttempts = 20;
		}
		if (registrationIdentityMaxAttempts == 0) {
			registrationIdentityMaxAttempts = 5;
		}
		if (registrationWindow == null) {
			registrationWindow = Duration.ofHours(1);
		}
	}
}
