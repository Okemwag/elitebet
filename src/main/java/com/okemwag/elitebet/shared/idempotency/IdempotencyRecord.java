package com.okemwag.elitebet.shared.idempotency;

public record IdempotencyRecord(IdempotencyKey key, IdempotentOperationType operationType) {
}
