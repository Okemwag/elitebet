package com.okemwag.elitebet.shared.events;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
	UUID eventId();

	Instant occurredAt();
}
