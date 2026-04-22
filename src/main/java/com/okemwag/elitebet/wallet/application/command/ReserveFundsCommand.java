package com.okemwag.elitebet.wallet.application.command;

import java.time.Instant;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;

public record ReserveFundsCommand(
		String principalId,
		Money amount,
		IdempotencyKey idempotencyKey,
		String referenceType,
		String referenceId,
		Instant expiresAt,
		String actorId) {
}
