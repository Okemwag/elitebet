package com.okemwag.elitebet.user.api.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertUserProfileRequest(
		@NotBlank @Size(max = 80) String firstName,
		@NotBlank @Size(max = 80) String lastName,
		@NotNull @Past LocalDate dateOfBirth,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
		@Size(max = 80) String regionCode,
		@NotBlank @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$") String phoneNumber) {
}
