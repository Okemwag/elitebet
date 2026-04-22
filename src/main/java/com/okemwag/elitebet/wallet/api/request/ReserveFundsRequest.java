package com.okemwag.elitebet.wallet.api.request;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReserveFundsRequest(
		@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode,
		@NotBlank @Size(max = 80) String referenceType,
		@NotBlank @Size(max = 160) String referenceId,
		@NotNull Instant expiresAt) {
}
