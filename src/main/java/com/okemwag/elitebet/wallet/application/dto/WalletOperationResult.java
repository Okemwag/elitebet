package com.okemwag.elitebet.wallet.application.dto;

import java.util.UUID;

public record WalletOperationResult(
		UUID walletId,
		UUID transactionId,
		UUID reservationId,
		String currencyCode,
		long balanceMinor,
		long reservedMinor,
		long availableMinor) {
}
