package com.okemwag.elitebet.shared.ratelimit;

import java.time.Duration;

public record RateLimitOutcome(boolean allowed, long limit, long remaining, Duration retryAfter) {
	public static RateLimitOutcome allowed(long limit, long remaining) {
		return new RateLimitOutcome(true, limit, Math.max(remaining, 0), Duration.ZERO);
	}

	public static RateLimitOutcome denied(long limit, Duration retryAfter) {
		return new RateLimitOutcome(false, limit, 0, retryAfter);
	}
}
