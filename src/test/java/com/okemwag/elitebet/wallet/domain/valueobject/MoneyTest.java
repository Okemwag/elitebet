package com.okemwag.elitebet.wallet.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency KES = Currency.getInstance("KES");

	private static final Currency USD = Currency.getInstance("USD");

	@Test
	void convertsMajorAmountToMinorUnitsUsingCurrencyScale() {
		Money money = Money.ofMajor(new BigDecimal("1250.75"), KES);

		assertThat(money.minorUnits()).isEqualTo(125075);
		assertThat(money.toMajor()).isEqualByComparingTo("1250.75");
	}

	@Test
	void rejectsAmountsThatRequireImplicitRounding() {
		assertThatThrownBy(() -> Money.ofMajor(new BigDecimal("10.005"), USD))
			.isInstanceOf(ArithmeticException.class);
	}

	@Test
	void addsAndSubtractsOnlyMatchingCurrencies() {
		Money first = Money.ofMinor(5000, KES);
		Money second = Money.ofMinor(1250, KES);

		assertThat(first.add(second).minorUnits()).isEqualTo(6250);
		assertThat(first.subtract(second).minorUnits()).isEqualTo(3750);
		assertThatThrownBy(() -> first.add(Money.ofMinor(100, USD)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("currency mismatch");
	}

	@Test
	void validatesPositiveAndNonNegativeAmounts() {
		assertThat(Money.ofMinor(1, KES).requirePositive("stake")).isEqualTo(Money.ofMinor(1, KES));
		assertThat(Money.zero(KES).requireNonNegative("balance")).isEqualTo(Money.zero(KES));
		assertThatThrownBy(() -> Money.zero(KES).requirePositive("stake"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("stake must be greater than zero");
		assertThatThrownBy(() -> Money.ofMinor(-1, KES).requireNonNegative("balance"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("balance must not be negative");
	}
}
