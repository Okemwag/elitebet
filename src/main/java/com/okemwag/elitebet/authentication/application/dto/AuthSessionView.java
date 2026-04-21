package com.okemwag.elitebet.authentication.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record AuthSessionView(
		boolean active,
		String subject,
		String issuer,
		List<String> audience,
		Instant issuedAt,
		Instant expiresAt,
		Instant authenticatedAt,
		Set<String> roles,
		Set<String> scopes) {
}
