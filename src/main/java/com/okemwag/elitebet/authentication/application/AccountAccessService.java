package com.okemwag.elitebet.authentication.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakAdminClient;
import com.okemwag.elitebet.authentication.mapper.AuthenticationMapper;
import com.okemwag.elitebet.shared.exception.ExternalIntegrationException;
import com.okemwag.elitebet.shared.exception.NotFoundException;

@Service
public class AccountAccessService {

	private final AuthAccountRepository repository;

	private final ObjectProvider<KeycloakAdminClient> keycloakAdminClient;

	private final AuthenticationMapper mapper;

	private final Clock clock;

	public AccountAccessService(AuthAccountRepository repository, ObjectProvider<KeycloakAdminClient> keycloakAdminClient,
			AuthenticationMapper mapper, Clock clock) {
		this.repository = repository;
		this.keycloakAdminClient = keycloakAdminClient;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AuthAccountView getByPrincipalId(String principalId) {
		return mapper.toView(find(principalId), Set.of());
	}

	@Transactional
	public AuthAccountView lock(String principalId, Instant lockedUntil) {
		AuthAccount account = find(principalId);
		account.lock(lockedUntil, clock.instant());
		keycloakClient().setUserEnabled(account.providerUserId(), false);
		keycloakClient().logoutUserSessions(account.providerUserId());
		return mapper.toView(account, Set.of());
	}

	@Transactional
	public AuthAccountView unlock(String principalId) {
		AuthAccount account = find(principalId);
		account.unlock(clock.instant());
		keycloakClient().setUserEnabled(account.providerUserId(), true);
		return mapper.toView(account, Set.of());
	}

	@Transactional
	public AuthAccountView disable(String principalId) {
		AuthAccount account = find(principalId);
		account.disable(clock.instant());
		keycloakClient().setUserEnabled(account.providerUserId(), false);
		keycloakClient().logoutUserSessions(account.providerUserId());
		return mapper.toView(account, Set.of());
	}

	private AuthAccount find(String principalId) {
		return repository.findByPrincipalId(principalId)
			.orElseThrow(() -> new NotFoundException("Authentication account not found"));
	}

	private KeycloakAdminClient keycloakClient() {
		KeycloakAdminClient client = keycloakAdminClient.getIfAvailable();
		if (client == null) {
			throw new ExternalIntegrationException("Keycloak admin client is not configured");
		}
		return client;
	}
}
