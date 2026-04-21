package com.okemwag.elitebet.authentication.application.dto;

import java.time.Instant;
import java.util.Set;

import com.okemwag.elitebet.authentication.domain.enums.AccountStatus;
import com.okemwag.elitebet.authentication.domain.enums.MfaStatus;

public record AuthAccountView(
		String principalId,
		String username,
		String email,
		boolean emailVerified,
		AccountStatus status,
		MfaStatus mfaStatus,
		Set<String> roles,
		Instant lockedUntil,
		Instant lastLoginAt) {
}
