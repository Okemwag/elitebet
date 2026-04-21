package com.okemwag.elitebet.shared.auditing;

public interface AuditEventPublisher {
	void publish(AuditAction action, AuditCategory category, AuditMetadata metadata);
}
