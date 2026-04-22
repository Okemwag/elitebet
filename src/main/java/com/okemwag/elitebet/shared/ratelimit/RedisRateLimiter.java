package com.okemwag.elitebet.shared.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiter implements RateLimiter {

	private static final String KEY_PREFIX = "elitebet:rate-limit:";

	private final StringRedisTemplate redisTemplate;

	public RedisRateLimiter(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public RateLimitOutcome consume(RateLimitRule rule, String subject) {
		String key = KEY_PREFIX + rule.name() + ":" + RateLimitKeyHasher.hash(subject);
		Long attempts = redisTemplate.opsForValue().increment(key);
		if (attempts == null) {
			return RateLimitOutcome.denied(rule.maxAttempts(), rule.window());
		}
		if (attempts == 1 || ttlSeconds(key) < 0) {
			redisTemplate.expire(key, rule.window());
		}
		if (attempts <= rule.maxAttempts()) {
			return RateLimitOutcome.allowed(rule.maxAttempts(), rule.maxAttempts() - attempts);
		}
		return RateLimitOutcome.denied(rule.maxAttempts(), retryAfter(key, rule.window()));
	}

	private long ttlSeconds(String key) {
		Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
		return ttl == null ? -1 : ttl;
	}

	private Duration retryAfter(String key, Duration fallback) {
		long ttl = ttlSeconds(key);
		if (ttl < 1) {
			return fallback;
		}
		return Duration.ofSeconds(ttl);
	}
}
