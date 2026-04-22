package com.okemwag.elitebet.wallet.api.request;

import java.math.BigDecimal;

import com.okemwag.elitebet.wallet.domain.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreditWalletRequest(
		@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode,
		@NotNull TransactionType transactionType,
		@Size(max = 80) String referenceType,
		@Size(max = 160) String referenceId,
		@Size(max = 160) String externalReference,
		@NotBlank @Size(max = 500) String reason) {
}
