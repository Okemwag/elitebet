package com.okemwag.elitebet.shared.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.shared.exception.ConflictException;

@Service
@ConditionalOnBean(IdempotencyRecordRepository.class)
public class IdempotencyService {

	private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(10);

	private final IdempotencyRecordRepository repository;

	private final Clock clock;

	public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public IdempotencyRecord startOrGet(IdempotentOperationType operationType, IdempotencyKey idempotencyKey,
			String actorId, String requestHash) {
		Optional<IdempotencyRecord> existing = repository.findByOperationTypeAndIdempotencyKeyAndActorId(operationType,
				idempotencyKey.value(), actorId);
		Instant now = clock.instant();
		return existing.map(record -> reuseOrRestart(record, requestHash, now))
			.orElseGet(() -> repository.save(IdempotencyRecord.start(operationType, idempotencyKey, actorId,
					requestHash, now.plus(DEFAULT_LOCK_DURATION), now)));
	}

	@Transactional
	public void complete(IdempotencyRecord record, int responseStatus, String responseBody) {
		record.complete(responseStatus, responseBody, clock.instant());
	}

	@Transactional
	public void fail(IdempotencyRecord record, String failureReason) {
		record.fail(failureReason, clock.instant());
	}

	private IdempotencyRecord reuseOrRestart(IdempotencyRecord record, String requestHash, Instant now) {
		if (!record.requestHash().equals(requestHash)) {
			throw new ConflictException("Idempotency key has already been used with a different request payload");
		}
		if (record.status() == IdempotencyStatus.COMPLETED) {
			return record;
		}
		if (record.status() == IdempotencyStatus.IN_PROGRESS && record.lockedUntil() != null
				&& record.lockedUntil().isAfter(now)) {
			throw new ConflictException("Idempotent operation is already in progress");
		}
		record.restart(now.plus(DEFAULT_LOCK_DURATION), now);
		return record;
	}
}
