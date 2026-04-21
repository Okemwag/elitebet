package com.okemwag.elitebet.authentication.api.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountLockRequest(
		@NotNull Instant lockedUntil,
		@NotBlank @Size(max = 500) String reason) {
}
