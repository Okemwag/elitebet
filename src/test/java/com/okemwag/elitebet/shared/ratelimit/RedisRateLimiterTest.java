package com.okemwag.elitebet.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	void firstAttemptIsAllowedAndSetsExpiry() {
		RedisRateLimiter rateLimiter = rateLimiter();
		RateLimitRule rule = new RateLimitRule("auth:registration:ip", 2, Duration.ofMinutes(5));
		when(valueOperations.increment(any())).thenReturn(1L);

		RateLimitOutcome outcome = rateLimiter.consume(rule, "127.0.0.1");

		assertThat(outcome.allowed()).isTrue();
		assertThat(outcome.remaining()).isEqualTo(1);
		verify(redisTemplate).expire(any(), eq(Duration.ofMinutes(5)));
	}

	@Test
	void overLimitAttemptIsDeniedWithRetryAfter() {
		RedisRateLimiter rateLimiter = rateLimiter();
		RateLimitRule rule = new RateLimitRule("auth:registration:identity", 2, Duration.ofHours(1));
		when(valueOperations.increment(any())).thenReturn(3L);
		when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenReturn(120L);

		RateLimitOutcome outcome = rateLimiter.consume(rule, "bettor@example.com");

		assertThat(outcome.allowed()).isFalse();
		assertThat(outcome.limit()).isEqualTo(2);
		assertThat(outcome.retryAfter()).isEqualTo(Duration.ofMinutes(2));
	}

	@Test
	void missingRedisIncrementResultDeniesRequest() {
		RedisRateLimiter rateLimiter = rateLimiter();
		RateLimitRule rule = new RateLimitRule("auth:registration:identity", 2, Duration.ofHours(1));
		when(valueOperations.increment(any())).thenReturn(null);

		RateLimitOutcome outcome = rateLimiter.consume(rule, "bettor@example.com");

		assertThat(outcome.allowed()).isFalse();
		assertThat(outcome.retryAfter()).isEqualTo(Duration.ofHours(1));
	}

	private RedisRateLimiter rateLimiter() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		return new RedisRateLimiter(redisTemplate);
	}
}
