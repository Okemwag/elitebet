package com.okemwag.elitebet.user.application.dto;

import java.time.LocalDate;

import com.okemwag.elitebet.user.domain.enums.UserProfileStatus;

public record UserProfileView(
		String principalId,
		String firstName,
		String lastName,
		LocalDate dateOfBirth,
		String countryCode,
		String regionCode,
		String phoneNumber,
		UserProfileStatus status) {
}
