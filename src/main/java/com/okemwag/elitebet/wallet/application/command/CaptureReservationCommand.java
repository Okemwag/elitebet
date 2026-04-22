package com.okemwag.elitebet.wallet.application.command;

import java.util.UUID;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;

public record CaptureReservationCommand(
		UUID reservationId,
		IdempotencyKey idempotencyKey,
		IdempotentOperationType operationType,
		String actorId) {
}
