package com.okemwag.elitebet.user.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.shared.exception.NotFoundException;
import com.okemwag.elitebet.shared.exception.ValidationException;
import com.okemwag.elitebet.user.api.request.UpsertUserProfileRequest;
import com.okemwag.elitebet.user.application.dto.UserProfileView;
import com.okemwag.elitebet.user.domain.model.UserProfile;
import com.okemwag.elitebet.user.domain.repository.UserProfileRepository;
import com.okemwag.elitebet.user.mapper.UserProfileMapper;

@Service
public class UserProfileService {

	private static final int MINIMUM_AGE_YEARS = 18;

	private final UserProfileRepository repository;

	private final UserProfileMapper mapper;

	private final Clock clock;

	public UserProfileService(UserProfileRepository repository, UserProfileMapper mapper, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public UserProfileView get(String principalId) {
		return repository.findByPrincipalId(principalId)
			.map(mapper::toView)
			.orElseThrow(() -> new NotFoundException("User profile not found"));
	}

	@Transactional
	public UpsertResult upsert(String principalId, UpsertUserProfileRequest request) {
		validateAge(request.dateOfBirth());
		var now = clock.instant();
		return repository.findByPrincipalId(principalId)
			.map(profile -> {
				profile.update(request.firstName(), request.lastName(), request.dateOfBirth(), request.countryCode(),
						request.regionCode(), request.phoneNumber(), now);
				return new UpsertResult(mapper.toView(repository.save(profile)), false);
			})
			.orElseGet(() -> {
				UserProfile profile = UserProfile.create(principalId, request.firstName(), request.lastName(),
						request.dateOfBirth(), request.countryCode(), request.regionCode(), request.phoneNumber(), now);
				return new UpsertResult(mapper.toView(repository.save(profile)), true);
			});
	}

	private void validateAge(LocalDate dateOfBirth) {
		if (Period.between(dateOfBirth, LocalDate.now(clock)).getYears() < MINIMUM_AGE_YEARS) {
			throw new ValidationException("User must be at least 18 years old");
		}
	}

	public record UpsertResult(UserProfileView profile, boolean created) {
	}
}
