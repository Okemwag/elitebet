package com.okemwag.elitebet.wallet.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
		@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode) {
}
