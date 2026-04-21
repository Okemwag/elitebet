package com.okemwag.elitebet.shared.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		return existing.orElseGet(() -> repository.save(IdempotencyRecord.start(operationType, idempotencyKey, actorId,
				requestHash, clock.instant().plus(DEFAULT_LOCK_DURATION), clock.instant())));
	}

	@Transactional
	public void complete(IdempotencyRecord record, int responseStatus, String responseBody) {
		record.complete(responseStatus, responseBody, clock.instant());
	}

	@Transactional
	public void fail(IdempotencyRecord record, String failureReason) {
		record.fail(failureReason, clock.instant());
	}
}
