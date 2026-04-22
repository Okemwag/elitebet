package com.okemwag.elitebet.wallet.application.command;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;
import com.okemwag.elitebet.wallet.domain.enums.TransactionType;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;

public record CreditWalletCommand(
		String principalId,
		Money amount,
		IdempotencyKey idempotencyKey,
		IdempotentOperationType operationType,
		TransactionType transactionType,
		String referenceType,
		String referenceId,
		String externalReference,
		String actorId) {
}
