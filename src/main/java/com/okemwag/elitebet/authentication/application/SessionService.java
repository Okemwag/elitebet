package com.okemwag.elitebet.authentication.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.okemwag.elitebet.authentication.application.dto.AuthSessionView;
import com.okemwag.elitebet.authentication.infrastructure.client.KeycloakRealmClient;
import com.okemwag.elitebet.shared.exception.ExternalIntegrationException;
import com.okemwag.elitebet.shared.security.RoleConstants;

@Service
public class SessionService {

	private final ObjectProvider<KeycloakRealmClient> keycloakRealmClient;

	public SessionService(ObjectProvider<KeycloakRealmClient> keycloakRealmClient) {
		this.keycloakRealmClient = keycloakRealmClient;
	}

	public AuthSessionView currentSession(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new IllegalStateException("No authenticated JWT session is available");
		}
		return new AuthSessionView(true, jwt.getSubject(), jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
				jwt.getAudience(), jwt.getIssuedAt(), jwt.getExpiresAt(), authenticatedAt(jwt), roles(authentication),
				scopes(jwt));
	}

	public void logout(String refreshToken) {
		KeycloakRealmClient client = keycloakRealmClient.getIfAvailable();
		if (client == null) {
			throw new ExternalIntegrationException("Keycloak realm client is not configured");
		}
		client.revokeRefreshToken(refreshToken);
	}

	private Instant authenticatedAt(Jwt jwt) {
		Object value = jwt.getClaim("auth_time");
		if (value instanceof Instant instant) {
			return instant;
		}
		if (value instanceof Number number) {
			return Instant.ofEpochSecond(number.longValue());
		}
		return null;
	}

	private Set<String> roles(Authentication authentication) {
		return authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority.startsWith(RoleConstants.ROLE_PREFIX))
			.map(authority -> authority.substring(RoleConstants.ROLE_PREFIX.length()))
			.collect(Collectors.toUnmodifiableSet());
	}

	private Set<String> scopes(Jwt jwt) {
		String scope = jwt.getClaimAsString("scope");
		if (scope == null || scope.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(scope.split(" "))
			.filter(value -> !value.isBlank())
			.collect(Collectors.toUnmodifiableSet());
	}
}
