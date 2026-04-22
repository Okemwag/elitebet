package com.okemwag.elitebet.authentication.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.authentication.api.request.RegisterAccountRequest;
import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.authentication.domain.enums.AuthProvider;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakAdminClient;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakUserRegistration;
import com.okemwag.elitebet.authentication.mapper.AuthenticationMapper;
import com.okemwag.elitebet.shared.exception.ConflictException;
import com.okemwag.elitebet.shared.exception.ExternalIntegrationException;
import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotencyRecord;
import com.okemwag.elitebet.shared.idempotency.IdempotencyService;
import com.okemwag.elitebet.shared.idempotency.IdempotencyStatus;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;
import com.okemwag.elitebet.shared.security.RoleConstants;

@Service
public class RegistrationService {

	private final AuthAccountRepository repository;

	private final ObjectProvider<KeycloakAdminClient> keycloakAdminClient;

	private final AuthenticationMapper mapper;

	private final Clock clock;

	private final ObjectProvider<IdempotencyService> idempotencyService;

	private final ObjectProvider<AccountNumberGenerator> accountNumberGenerator;

	private final ObjectMapper objectMapper;

	public RegistrationService(AuthAccountRepository repository, ObjectProvider<KeycloakAdminClient> keycloakAdminClient,
			AuthenticationMapper mapper, Clock clock, ObjectProvider<IdempotencyService> idempotencyService,
			ObjectProvider<AccountNumberGenerator> accountNumberGenerator, ObjectMapper objectMapper) {
		this.repository = repository;
		this.keycloakAdminClient = keycloakAdminClient;
		this.mapper = mapper;
		this.clock = clock;
		this.idempotencyService = idempotencyService;
		this.accountNumberGenerator = accountNumberGenerator;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public AuthAccountView register(RegisterAccountRequest request) {
		return createAccount(request);
	}

	@Transactional
	public AuthAccountView register(RegisterAccountRequest request, IdempotencyKey idempotencyKey) {
		IdempotencyService service = idempotencyService.getIfAvailable();
		if (service == null) {
			return createAccount(request);
		}
		IdempotencyRecord record = service.startOrGet(IdempotentOperationType.USER_REGISTRATION, idempotencyKey,
				normalizeEmail(request.email()), requestHash(request));
		if (record.status() == IdempotencyStatus.COMPLETED) {
			return readCompletedResponse(record);
		}
		try {
			AuthAccountView account = createAccount(request);
			service.complete(record, 201, toJson(account));
			return account;
		}
		catch (RuntimeException exception) {
			service.fail(record, exception.getMessage());
			throw exception;
		}
	}

	private AuthAccountView createAccount(RegisterAccountRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		if (repository.existsByEmail(normalizedEmail)) {
			throw new ConflictException("An account with this email already exists");
		}
		KeycloakAdminClient client = keycloakAdminClient.getIfAvailable();
		if (client == null) {
			throw new ExternalIntegrationException("Keycloak admin client is not configured");
		}
		String providerUserId = client.createUser(new KeycloakUserRegistration(request.username().trim(), normalizedEmail,
				request.password(), false));
		client.assignRealmRole(providerUserId, RoleConstants.BETTOR);
		String accountNumber = nextAccountNumber();
		AuthAccount account = AuthAccount.create(providerUserId, accountNumber, request.username().trim(),
				normalizedEmail, AuthProvider.KEYCLOAK, providerUserId, false, clock.instant());
		return mapper.toView(repository.save(account), Set.of(RoleConstants.BETTOR));
	}

	private String nextAccountNumber() {
		AccountNumberGenerator generator = accountNumberGenerator.getIfAvailable();
		if (generator == null) {
			throw new IllegalStateException("Account number generator is not configured");
		}
		return generator.nextAccountNumber();
	}

	private String requestHash(RegisterAccountRequest request) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest((normalizeEmail(request.email()) + "|" + request.username().trim()).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private String toJson(AuthAccountView account) {
		try {
			return objectMapper.writeValueAsString(account);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to serialize idempotent registration response", exception);
		}
	}

	private AuthAccountView readCompletedResponse(IdempotencyRecord record) {
		try {
			return objectMapper.readValue(record.responseBody(), AuthAccountView.class);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to deserialize idempotent registration response", exception);
		}
	}
}
