package com.okemwag.elitebet.shared.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, OutboxEventType type, OutboxStatus status, Instant occurredAt) {
}
