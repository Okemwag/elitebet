package com.okemwag.elitebet.shared.events;

import java.time.Instant;
import java.util.UUID;

public interface IntegrationEvent {
	UUID eventId();

	Instant occurredAt();
}
