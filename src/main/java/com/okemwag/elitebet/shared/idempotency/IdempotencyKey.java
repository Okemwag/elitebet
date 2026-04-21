package com.okemwag.elitebet.shared.idempotency;

public record IdempotencyKey(String value) {
	public IdempotencyKey {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Idempotency key is required");
		}
		if (value.length() > 160) {
			throw new IllegalArgumentException("Idempotency key must not exceed 160 characters");
		}
	}
}
