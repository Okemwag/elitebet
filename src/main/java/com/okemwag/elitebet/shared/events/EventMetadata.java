package com.okemwag.elitebet.shared.events;

public record EventMetadata(String correlationId, String causationId, String actorId) {
}
