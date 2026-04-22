package com.okemwag.elitebet.shared.ratelimit;

public interface RateLimiter {
	RateLimitOutcome consume(RateLimitRule rule, String subject);
}
