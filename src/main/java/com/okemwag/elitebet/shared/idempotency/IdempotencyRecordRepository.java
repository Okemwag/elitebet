package com.okemwag.elitebet.shared.idempotency;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
	Optional<IdempotencyRecord> findByOperationTypeAndIdempotencyKeyAndActorId(
			IdempotentOperationType operationType, String idempotencyKey, String actorId);
}
