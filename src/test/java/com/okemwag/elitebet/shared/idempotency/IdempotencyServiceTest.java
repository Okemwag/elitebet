package com.okemwag.elitebet.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.okemwag.elitebet.shared.exception.ConflictException;

class IdempotencyServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-22T00:00:00Z");

	private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);

	private final IdempotencyService service = new IdempotencyService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void createsNewRecordWhenKeyHasNotBeenUsed() {
		when(repository.findByOperationTypeAndIdempotencyKeyAndActorId(
				IdempotentOperationType.BET_PLACEMENT, "bet-key-1", "user-1")).thenReturn(Optional.empty());
		when(repository.save(any(IdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IdempotencyRecord record = service.startOrGet(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a");

		assertThat(record.status()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
		assertThat(record.lockedUntil()).isEqualTo(NOW.plusSeconds(600));
		verify(repository).save(record);
	}

	@Test
	void rejectsSameKeyWithDifferentRequestHash() {
		IdempotencyRecord existing = IdempotencyRecord.start(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a", NOW.plusSeconds(600), NOW);
		when(repository.findByOperationTypeAndIdempotencyKeyAndActorId(
				IdempotentOperationType.BET_PLACEMENT, "bet-key-1", "user-1")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.startOrGet(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-b"))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("different request payload");
	}

	@Test
	void blocksDuplicateInProgressOperationUntilLockExpires() {
		IdempotencyRecord existing = IdempotencyRecord.start(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a", NOW.plusSeconds(600), NOW);
		when(repository.findByOperationTypeAndIdempotencyKeyAndActorId(
				IdempotentOperationType.BET_PLACEMENT, "bet-key-1", "user-1")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.startOrGet(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a"))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("already in progress");
	}

	@Test
	void returnsCompletedRecordForReplayWithSameRequestHash() {
		IdempotencyRecord existing = IdempotencyRecord.start(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a", NOW.plusSeconds(600), NOW);
		existing.complete(201, "{\"id\":\"bet-1\"}", NOW);
		when(repository.findByOperationTypeAndIdempotencyKeyAndActorId(
				IdempotentOperationType.BET_PLACEMENT, "bet-key-1", "user-1")).thenReturn(Optional.of(existing));

		IdempotencyRecord record = service.startOrGet(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a");

		assertThat(record.status()).isEqualTo(IdempotencyStatus.COMPLETED);
		assertThat(record.responseStatus()).isEqualTo(201);
		assertThat(record.responseBody()).isEqualTo("{\"id\":\"bet-1\"}");
	}

	@Test
	void restartsFailedRecordForRetryWithSameRequestHash() {
		IdempotencyRecord existing = IdempotencyRecord.start(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a", NOW.minusSeconds(60), NOW.minusSeconds(900));
		existing.fail("temporary failure", NOW.minusSeconds(300));
		when(repository.findByOperationTypeAndIdempotencyKeyAndActorId(
				IdempotentOperationType.BET_PLACEMENT, "bet-key-1", "user-1")).thenReturn(Optional.of(existing));

		IdempotencyRecord record = service.startOrGet(IdempotentOperationType.BET_PLACEMENT,
				new IdempotencyKey("bet-key-1"), "user-1", "hash-a");

		assertThat(record.status()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
		assertThat(record.lockedUntil()).isEqualTo(NOW.plusSeconds(600));
	}
}
