package com.okemwag.elitebet.shared.idempotency;

public enum IdempotentOperationType {
	BET_PLACEMENT,
	DEPOSIT_CALLBACK,
	WITHDRAWAL_CALLBACK,
	SETTLEMENT_REPLAY,
	WALLET_ADJUSTMENT
}
