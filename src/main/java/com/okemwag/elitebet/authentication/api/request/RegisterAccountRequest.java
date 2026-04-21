package com.okemwag.elitebet.authentication.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
		@NotBlank @Size(max = 160) String username,
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(min = 12, max = 128) String password,
		@AssertTrue boolean termsAccepted,
		boolean marketingOptIn) {
}
