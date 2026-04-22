package com.okemwag.elitebet.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.okemwag.elitebet.authentication.api.request.RegisterAccountRequest;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakAdminClient;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakUserRegistration;
import com.okemwag.elitebet.authentication.mapper.AuthenticationMapper;
import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotencyRecord;
import com.okemwag.elitebet.shared.idempotency.IdempotencyRecordRepository;
import com.okemwag.elitebet.shared.idempotency.IdempotencyService;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;

class RegistrationServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-22T00:00:00Z");

	private final AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);

	private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);

	private final IdempotencyRecordRepository idempotencyRecordRepository = mock(IdempotencyRecordRepository.class);

	private final AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private final IdempotencyService idempotencyService = new IdempotencyService(idempotencyRecordRepository,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private final RegistrationService service = new RegistrationService(accountRepository, provider(keycloakAdminClient),
			new AuthenticationMapper(), Clock.fixed(NOW, ZoneOffset.UTC), provider(idempotencyService),
			provider(accountNumberGenerator), objectMapper);

	@Test
	void createsAccountAndCompletesIdempotencyRecord() {
		when(idempotencyRecordRepository.findByOperationTypeAndIdempotencyKeyAndActorId(
				any(IdempotentOperationType.class), any(), any())).thenReturn(Optional.empty());
		when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(accountRepository.existsByEmail("bettor@example.com")).thenReturn(false);
		when(keycloakAdminClient.createUser(any(KeycloakUserRegistration.class))).thenReturn("keycloak-user-1");
		when(accountNumberGenerator.nextAccountNumber()).thenReturn("EB100000000000");
		when(accountRepository.save(any(AuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var account = service.register(request(), new IdempotencyKey("registration-key-1"));

		assertThat(account.principalId()).isEqualTo("keycloak-user-1");
		assertThat(account.accountNumber()).isEqualTo("EB100000000000");
		verify(keycloakAdminClient).assignRealmRole("keycloak-user-1", "BETTOR");
	}

	@Test
	void replaysCompletedRegistrationWithoutCallingKeycloak() {
		IdempotencyRecord record = IdempotencyRecord.start(IdempotentOperationType.USER_REGISTRATION,
				new IdempotencyKey("registration-key-1"), "bettor@example.com", requestHash(),
				NOW.plusSeconds(600), NOW);
		record.complete(201, """
				{"principalId":"keycloak-user-1","accountNumber":"EB100000000000","username":"bettor1","email":"bettor@example.com","emailVerified":false,"status":"PENDING_ACTIVATION","mfaStatus":"NOT_CONFIGURED","roles":["BETTOR"],"lockedUntil":null,"lastLoginAt":null}
				""", NOW);
		when(idempotencyRecordRepository.findByOperationTypeAndIdempotencyKeyAndActorId(
				any(IdempotentOperationType.class), any(), any())).thenReturn(Optional.of(record));

		var account = service.register(request(), new IdempotencyKey("registration-key-1"));

		assertThat(account.principalId()).isEqualTo("keycloak-user-1");
		assertThat(account.accountNumber()).isEqualTo("EB100000000000");
		verify(keycloakAdminClient, never()).createUser(any());
	}

	private RegisterAccountRequest request() {
		return new RegisterAccountRequest("bettor1", "bettor@example.com", "correct-horse-password", true, false);
	}

	private String requestHash() {
		return java.util.HexFormat.of().formatHex(digest("bettor@example.com|bettor1"));
	}

	private byte[] digest(String value) {
		try {
			return java.security.MessageDigest.getInstance("SHA-256")
				.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		catch (java.security.NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static <T> ObjectProvider<T> provider(T value) {
		return new ObjectProvider<>() {
			@Override
			public T getObject(Object... args) {
				return value;
			}

			@Override
			public T getIfAvailable() {
				return value;
			}

			@Override
			public T getIfUnique() {
				return value;
			}

			@Override
			public T getObject() {
				return value;
			}
		};
	}
}
