package com.okemwag.elitebet.authentication.infrastructure.client;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.okemwag.elitebet.shared.exception.ExternalIntegrationException;

@Component
public class KeycloakRestClient implements KeycloakAdminClient, KeycloakRealmClient {

	private static final long TOKEN_EXPIRY_SKEW_SECONDS = 30;

	private final RestClient restClient;

	private final KeycloakProperties properties;

	private final Clock clock;

	private volatile AdminToken cachedAdminToken;

	public KeycloakRestClient(RestClient.Builder restClientBuilder, KeycloakProperties properties, Clock clock) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public String createUser(KeycloakUserRegistration registration) {
		UserCreateRequest request = new UserCreateRequest(registration.username(), registration.email(),
				registration.emailVerified(), true,
				List.of(new CredentialRequest("password", registration.password(), false)));
		try {
			URI location = restClient.post()
				.uri("/admin/realms/{realm}/users", properties.realm())
				.header(HttpHeaders.AUTHORIZATION, bearerAdminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity()
				.getHeaders()
				.getLocation();
			if (location == null) {
				throw new ExternalIntegrationException("Keycloak did not return a created user location");
			}
			return parseCreatedUserId(location);
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("create user", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to create Keycloak user");
		}
	}

	@Override
	public void assignRealmRole(String providerUserId, String role) {
		RoleRepresentation roleRepresentation = realmRole(role);
		try {
			restClient.post()
				.uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), providerUserId)
				.header(HttpHeaders.AUTHORIZATION, bearerAdminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(List.of(roleRepresentation))
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("assign realm role", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to assign Keycloak realm role");
		}
	}

	@Override
	public void setUserEnabled(String providerUserId, boolean enabled) {
		try {
			restClient.put()
				.uri("/admin/realms/{realm}/users/{userId}", properties.realm(), providerUserId)
				.header(HttpHeaders.AUTHORIZATION, bearerAdminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("enabled", enabled))
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("update user enabled state", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to update Keycloak user state");
		}
	}

	@Override
	public void logoutUserSessions(String providerUserId) {
		try {
			restClient.post()
				.uri("/admin/realms/{realm}/users/{userId}/logout", properties.realm(), providerUserId)
				.header(HttpHeaders.AUTHORIZATION, bearerAdminToken())
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("logout user sessions", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to logout Keycloak user sessions");
		}
	}

	@Override
	public void revokeRefreshToken(String refreshToken) {
		MultiValueMap<String, String> form = clientCredentialsForm();
		form.add("token", refreshToken);
		form.add("token_type_hint", "refresh_token");
		try {
			restClient.post()
				.uri("/realms/{realm}/protocol/openid-connect/revoke", properties.realm())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("revoke refresh token", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to revoke Keycloak refresh token");
		}
	}

	private RoleRepresentation realmRole(String role) {
		try {
			return restClient.get()
				.uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role)
				.header(HttpHeaders.AUTHORIZATION, bearerAdminToken())
				.retrieve()
				.body(RoleRepresentation.class);
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("load realm role", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to load Keycloak realm role");
		}
	}

	private String bearerAdminToken() {
		return "Bearer " + adminToken();
	}

	private String adminToken() {
		AdminToken token = cachedAdminToken;
		if (token != null && token.expiresAt().isAfter(clock.instant())) {
			return token.value();
		}
		synchronized (this) {
			token = cachedAdminToken;
			if (token != null && token.expiresAt().isAfter(clock.instant())) {
				return token.value();
			}
			TokenResponse response = requestAdminToken();
			Instant expiresAt = clock.instant().plusSeconds(Math.max(0, response.expiresIn() - TOKEN_EXPIRY_SKEW_SECONDS));
			cachedAdminToken = new AdminToken(response.accessToken(), expiresAt);
			return response.accessToken();
		}
	}

	private TokenResponse requestAdminToken() {
		try {
			return restClient.post()
				.uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(clientCredentialsForm())
				.retrieve()
				.body(TokenResponse.class);
		}
		catch (RestClientResponseException exception) {
			throw keycloakFailure("request admin token", exception);
		}
		catch (RestClientException exception) {
			throw new ExternalIntegrationException("Unable to obtain Keycloak admin token");
		}
	}

	private MultiValueMap<String, String> clientCredentialsForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", properties.adminClientId());
		form.add("client_secret", properties.adminClientSecret());
		return form;
	}

	private String parseCreatedUserId(URI location) {
		String path = location.getPath();
		int index = path.lastIndexOf('/');
		if (index < 0 || index == path.length() - 1) {
			throw new ExternalIntegrationException("Keycloak returned an invalid created user location");
		}
		return path.substring(index + 1);
	}

	private ExternalIntegrationException keycloakFailure(String operation, RestClientResponseException exception) {
		return new ExternalIntegrationException(
				"Keycloak failed to " + operation + " with status " + exception.getStatusCode().value());
	}

	private record AdminToken(String value, Instant expiresAt) {
	}

	private record TokenResponse(
			@com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
			@com.fasterxml.jackson.annotation.JsonProperty("expires_in") long expiresIn) {
	}

	private record UserCreateRequest(
			String username,
			String email,
			boolean emailVerified,
			boolean enabled,
			List<CredentialRequest> credentials) {
	}

	private record CredentialRequest(String type, String value, boolean temporary) {
	}

	private record RoleRepresentation(String id, String name) {
	}
}
