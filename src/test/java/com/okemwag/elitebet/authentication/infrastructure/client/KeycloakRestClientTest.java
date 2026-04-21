package com.okemwag.elitebet.authentication.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.okemwag.elitebet.shared.exception.ExternalIntegrationException;

class KeycloakRestClientTest {

	private static final String BASE_URL = "https://keycloak.example.test";

	private MockRestServiceServer server;

	private KeycloakRestClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new KeycloakRestClient(builder,
				new KeycloakProperties(BASE_URL, "elitebet", "elitebet-api", "secret"),
				Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void createsKeycloakUserAndReturnsProviderUserIdFromLocationHeader() {
		expectAdminToken();
		server.expect(requestTo(BASE_URL + "/admin/realms/elitebet/users"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andExpect(content().string(containsString("\"username\":\"bettor1\"")))
			.andExpect(content().string(containsString("\"email\":\"bettor@example.com\"")))
			.andRespond(withCreatedEntity(URI.create(BASE_URL + "/admin/realms/elitebet/users/keycloak-user-1")));

		String providerUserId = client.createUser(new KeycloakUserRegistration("bettor1", "bettor@example.com",
				"correct-horse-password", false));

		assertThat(providerUserId).isEqualTo("keycloak-user-1");
		server.verify();
	}

	@Test
	void assignsRealmRoleUsingKeycloakRoleRepresentation() {
		expectAdminToken();
		server.expect(requestTo(BASE_URL + "/admin/realms/elitebet/roles/BETTOR"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"BETTOR\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE_URL + "/admin/realms/elitebet/users/keycloak-user-1/role-mappings/realm"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andExpect(content().string(containsString("\"id\":\"role-1\"")))
			.andExpect(content().string(containsString("\"name\":\"BETTOR\"")))
			.andRespond(withNoContent());

		client.assignRealmRole("keycloak-user-1", "BETTOR");

		server.verify();
	}

	@Test
	void disablesUserAndLogsOutSessions() {
		expectAdminToken();
		server.expect(requestTo(BASE_URL + "/admin/realms/elitebet/users/keycloak-user-1"))
			.andExpect(method(HttpMethod.PUT))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andExpect(content().string(containsString("\"enabled\":false")))
			.andRespond(withNoContent());
		server.expect(requestTo(BASE_URL + "/admin/realms/elitebet/users/keycloak-user-1/logout"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andRespond(withNoContent());

		client.setUserEnabled("keycloak-user-1", false);
		client.logoutUserSessions("keycloak-user-1");

		server.verify();
	}

	@Test
	void revokesRefreshTokenWithClientCredentials() {
		server.expect(requestTo(BASE_URL + "/realms/elitebet/protocol/openid-connect/revoke"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("client_id=elitebet-api")))
			.andExpect(content().string(containsString("client_secret=secret")))
			.andExpect(content().string(containsString("token=refresh-token-1")))
			.andRespond(withNoContent());

		client.revokeRefreshToken("refresh-token-1");

		server.verify();
	}

	@Test
	void mapsKeycloakFailuresToExternalIntegrationExceptionWithoutLeakingSecrets() {
		server.expect(requestTo(BASE_URL + "/realms/elitebet/protocol/openid-connect/token"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		assertThatThrownBy(() -> client.createUser(new KeycloakUserRegistration("bettor1", "bettor@example.com",
				"correct-horse-password", false)))
			.isInstanceOf(ExternalIntegrationException.class)
			.hasMessageContaining("Keycloak failed to request admin token")
			.hasMessageNotContaining("secret")
			.hasMessageNotContaining("correct-horse-password");

		server.verify();
	}

	private void expectAdminToken() {
		server.expect(requestTo(BASE_URL + "/realms/elitebet/protocol/openid-connect/token"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("grant_type=client_credentials")))
			.andExpect(content().string(containsString("client_id=elitebet-api")))
			.andExpect(content().string(containsString("client_secret=secret")))
			.andRespond(withSuccess("{\"access_token\":\"admin-token\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
	}
}
