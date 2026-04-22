package com.okemwag.elitebet.wallet.domain.valueobject;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public record CurrencyCode(String value) {

	public CurrencyCode {
		Objects.requireNonNull(value, "currency code is required");
		value = value.trim().toUpperCase(Locale.ROOT);
		if (value.length() != 3) {
			throw new IllegalArgumentException("currency code must be ISO-4217 alpha-3");
		}
		Currency.getInstance(value);
	}

	public Currency toCurrency() {
		return Currency.getInstance(value);
	}
}
