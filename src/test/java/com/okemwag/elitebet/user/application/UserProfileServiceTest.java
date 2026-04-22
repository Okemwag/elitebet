package com.okemwag.elitebet.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.okemwag.elitebet.shared.exception.ValidationException;
import com.okemwag.elitebet.user.api.request.UpsertUserProfileRequest;
import com.okemwag.elitebet.user.domain.model.UserProfile;
import com.okemwag.elitebet.user.domain.repository.UserProfileRepository;
import com.okemwag.elitebet.user.mapper.UserProfileMapper;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private UserProfileRepository repository;

	@Test
	void createsProfileForPrincipalAndNormalizesFields() {
		UserProfileService service = service();
		when(repository.findByPrincipalId("user-1")).thenReturn(Optional.empty());
		when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = service.upsert("user-1", new UpsertUserProfileRequest(" Ada ", " Lovelace ",
				LocalDate.parse("1990-01-01"), "us", "ny", "+15551234567"));

		assertThat(result.created()).isTrue();
		assertThat(result.profile().principalId()).isEqualTo("user-1");
		assertThat(result.profile().firstName()).isEqualTo("Ada");
		assertThat(result.profile().countryCode()).isEqualTo("US");
		assertThat(result.profile().regionCode()).isEqualTo("NY");
	}

	@Test
	void updatesExistingProfile() {
		UserProfileService service = service();
		UserProfile existing = UserProfile.create("user-1", "Ada", "Lovelace", LocalDate.parse("1990-01-01"), "GB",
				null, "+447700900123", CLOCK.instant());
		when(repository.findByPrincipalId("user-1")).thenReturn(Optional.of(existing));
		when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = service.upsert("user-1", new UpsertUserProfileRequest("Grace", "Hopper",
				LocalDate.parse("1980-12-09"), "us", "va", "+15557654321"));

		assertThat(result.created()).isFalse();
		assertThat(result.profile().firstName()).isEqualTo("Grace");
		assertThat(result.profile().lastName()).isEqualTo("Hopper");
		assertThat(result.profile().countryCode()).isEqualTo("US");
	}

	@Test
	void rejectsUnderageProfile() {
		UserProfileService service = service();

		assertThatThrownBy(() -> service.upsert("user-1", new UpsertUserProfileRequest("Young", "User",
				LocalDate.parse("2010-01-01"), "US", null, "+15551234567")))
			.isInstanceOf(ValidationException.class)
			.hasMessage("User must be at least 18 years old");
	}

	private UserProfileService service() {
		return new UserProfileService(repository, new UserProfileMapper(), CLOCK);
	}
}
