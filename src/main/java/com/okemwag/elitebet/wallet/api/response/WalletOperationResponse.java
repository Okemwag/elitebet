package com.okemwag.elitebet.wallet.api.response;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import com.okemwag.elitebet.wallet.application.dto.WalletOperationResult;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;

public record WalletOperationResponse(
		UUID walletId,
		UUID transactionId,
		UUID reservationId,
		String currencyCode,
		long balanceMinor,
		long reservedMinor,
		long availableMinor,
		BigDecimal balance,
		BigDecimal reserved,
		BigDecimal available) {

	public static WalletOperationResponse from(WalletOperationResult result) {
		Currency currency = Currency.getInstance(result.currencyCode());
		return new WalletOperationResponse(result.walletId(), result.transactionId(), result.reservationId(),
				result.currencyCode(), result.balanceMinor(), result.reservedMinor(), result.availableMinor(),
				Money.ofMinor(result.balanceMinor(), currency).toMajor(),
				Money.ofMinor(result.reservedMinor(), currency).toMajor(),
				Money.ofMinor(result.availableMinor(), currency).toMajor());
	}
}
