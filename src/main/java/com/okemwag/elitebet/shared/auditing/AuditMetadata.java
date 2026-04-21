package com.okemwag.elitebet.shared.auditing;

import java.util.Map;

public record AuditMetadata(
		String entityType,
		String entityId,
		String beforeState,
		String afterState,
		Map<String, String> values) {
}
