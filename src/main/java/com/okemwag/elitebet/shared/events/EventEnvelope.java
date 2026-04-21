package com.okemwag.elitebet.shared.events;

public record EventEnvelope<T>(T payload, EventMetadata metadata) {
}
