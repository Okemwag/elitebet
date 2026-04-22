package com.okemwag.elitebet.user.mapper;

import org.springframework.stereotype.Component;

import com.okemwag.elitebet.user.application.dto.UserProfileView;
import com.okemwag.elitebet.user.domain.model.UserProfile;

@Component
public class UserProfileMapper {

	public UserProfileView toView(UserProfile profile) {
		return new UserProfileView(profile.principalId(), profile.firstName(), profile.lastName(),
				profile.dateOfBirth(), profile.countryCode(), profile.regionCode(), profile.phoneNumber(),
				profile.status());
	}
}
