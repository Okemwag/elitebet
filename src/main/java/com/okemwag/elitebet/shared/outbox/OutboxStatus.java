package com.okemwag.elitebet.shared.outbox;

public enum OutboxStatus {
	PENDING,
	PROCESSING,
	PUBLISHED,
	FAILED
}
