package com.okemwag.elitebet.shared.auditing;

public record AuditContext(String actorId, AuditActorType actorType, String correlationId) {
}
