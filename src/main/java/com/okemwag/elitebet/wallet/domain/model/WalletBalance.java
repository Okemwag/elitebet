package com.okemwag.elitebet.wallet.domain.model;

import java.util.Objects;

import com.okemwag.elitebet.wallet.domain.valueobject.Money;

public record WalletBalance(Money total, Money reserved) {

	public WalletBalance {
		Objects.requireNonNull(total, "total balance is required").requireNonNegative("total balance");
		Objects.requireNonNull(reserved, "reserved balance is required").requireNonNegative("reserved balance");
		if (!total.currency().equals(reserved.currency())) {
			throw new IllegalArgumentException("wallet balance currencies must match");
		}
		if (reserved.compareTo(total) > 0) {
			throw new IllegalArgumentException("reserved balance cannot exceed total balance");
		}
	}

	public Money available() {
		return total.subtract(reserved);
	}
}
