package com.okemwag.elitebet.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.okemwag.elitebet.wallet.domain.valueobject.Money;

class WalletBalanceTest {

	private static final Currency KES = Currency.getInstance("KES");

	private static final Currency USD = Currency.getInstance("USD");

	@Test
	void calculatesAvailableBalanceFromTotalAndReservedMoney() {
		WalletBalance balance = new WalletBalance(Money.ofMinor(10_000, KES), Money.ofMinor(2_500, KES));

		assertThat(balance.available()).isEqualTo(Money.ofMinor(7_500, KES));
	}

	@Test
	void rejectsReservedBalanceAboveTotalBalance() {
		assertThatThrownBy(() -> new WalletBalance(Money.ofMinor(1_000, KES), Money.ofMinor(1_001, KES)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("reserved balance cannot exceed total balance");
	}

	@Test
	void rejectsMixedCurrencies() {
		assertThatThrownBy(() -> new WalletBalance(Money.ofMinor(1_000, KES), Money.ofMinor(100, USD)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("wallet balance currencies must match");
	}
}
