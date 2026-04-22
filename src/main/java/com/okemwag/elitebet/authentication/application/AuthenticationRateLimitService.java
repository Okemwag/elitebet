package com.okemwag.elitebet.authentication.application;

import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.okemwag.elitebet.authentication.api.request.RegisterAccountRequest;
import com.okemwag.elitebet.shared.exception.RateLimitExceededException;
import com.okemwag.elitebet.shared.ratelimit.RateLimitOutcome;
import com.okemwag.elitebet.shared.ratelimit.RateLimitRule;
import com.okemwag.elitebet.shared.ratelimit.RateLimiter;

@Service
public class AuthenticationRateLimitService {

	private final RateLimiter rateLimiter;

	private final AuthenticationRateLimitProperties properties;

	private final AuthAuditService authAuditService;

	public AuthenticationRateLimitService(RateLimiter rateLimiter, AuthenticationRateLimitProperties properties,
			AuthAuditService authAuditService) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.authAuditService = authAuditService;
	}

	public void enforceRegistration(RegisterAccountRequest request, HttpServletRequest httpRequest) {
		RateLimitRule ipRule = new RateLimitRule("auth:registration:ip", properties.registrationIpMaxAttempts(),
				properties.registrationWindow());
		RateLimitRule identityRule = new RateLimitRule("auth:registration:identity",
				properties.registrationIdentityMaxAttempts(), properties.registrationWindow());

		List<RegistrationLimitCheck> checks = List.of(
				new RegistrationLimitCheck("ip", ipRule, httpRequest.getRemoteAddr()),
				new RegistrationLimitCheck("email", identityRule, request.email()),
				new RegistrationLimitCheck("username", identityRule, request.username()));

		RateLimitExceededException exceeded = null;
		for (RegistrationLimitCheck check : checks) {
			RateLimitOutcome outcome = rateLimiter.consume(check.rule(), check.subject());
			if (!outcome.allowed() && exceeded == null) {
				Duration retryAfter = outcome.retryAfter().isZero() ? properties.registrationWindow()
						: outcome.retryAfter();
				authAuditService.registrationThrottled(check.type(), retryAfter, httpRequest);
				exceeded = new RateLimitExceededException("Too many registration attempts", retryAfter);
			}
		}
		if (exceeded != null) {
			throw exceeded;
		}
	}

	private record RegistrationLimitCheck(String type, RateLimitRule rule, String subject) {
	}
}
