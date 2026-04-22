package com.okemwag.elitebet.shared.ratelimit;

import java.time.Duration;

public record RateLimitRule(String name, long maxAttempts, Duration window) {
	public RateLimitRule {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Rate limit name is required");
		}
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("Rate limit maxAttempts must be positive");
		}
		if (window == null || window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("Rate limit window must be positive");
		}
	}
}
