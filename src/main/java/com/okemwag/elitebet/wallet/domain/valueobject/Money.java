package com.okemwag.elitebet.wallet.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

	public Money {
		Objects.requireNonNull(currency, "currency is required");
		if (currency.getDefaultFractionDigits() < 0) {
			throw new IllegalArgumentException("currency must have a fixed decimal scale");
		}
	}

	public static Money ofMinor(long minorUnits, Currency currency) {
		return new Money(minorUnits, currency);
	}

	public static Money zero(Currency currency) {
		return new Money(0, currency);
	}

	public static Money ofMajor(BigDecimal amount, Currency currency) {
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(currency, "currency is required");
		int scale = currency.getDefaultFractionDigits();
		if (scale < 0) {
			throw new IllegalArgumentException("currency must have a fixed decimal scale");
		}
		BigDecimal scaled = amount.setScale(scale, RoundingMode.UNNECESSARY).movePointRight(scale);
		return new Money(scaled.longValueExact(), currency);
	}

	public BigDecimal toMajor() {
		return BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits());
	}

	public Money add(Money other) {
		requireSameCurrency(other);
		return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
	}

	public Money subtract(Money other) {
		requireSameCurrency(other);
		return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
	}

	public Money negate() {
		return new Money(Math.negateExact(minorUnits), currency);
	}

	public boolean isZero() {
		return minorUnits == 0;
	}

	public boolean isPositive() {
		return minorUnits > 0;
	}

	public boolean isNegative() {
		return minorUnits < 0;
	}

	public Money requirePositive(String fieldName) {
		if (!isPositive()) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
		return this;
	}

	public Money requireNonNegative(String fieldName) {
		if (isNegative()) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return this;
	}

	@Override
	public int compareTo(Money other) {
		requireSameCurrency(other);
		return Long.compare(minorUnits, other.minorUnits);
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other money is required");
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException("currency mismatch: " + currency.getCurrencyCode() + " != "
					+ other.currency.getCurrencyCode());
		}
	}
}
