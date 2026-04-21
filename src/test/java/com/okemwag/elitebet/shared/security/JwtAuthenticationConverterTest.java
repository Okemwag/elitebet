package com.okemwag.elitebet.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthenticationConverterTest {

	private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

	@Test
	void mapsRealmRolesResourceRolesAndScopes() {
		Jwt jwt = jwt(Map.of(
				"preferred_username", "bettor1",
				"realm_access", Map.of("roles", List.of("bettor", " admin ")),
				"resource_access", Map.of("elitebet-api", Map.of("roles", List.of("support"))),
				"scope", "openid profile"));

		var authentication = converter.convert(jwt);

		assertThat(authentication.getName()).isEqualTo("bettor1");
		assertThat(authentication.getAuthorities())
			.extracting("authority")
			.contains("ROLE_BETTOR", "ROLE_ADMIN", "ROLE_SUPPORT", "SCOPE_openid", "SCOPE_profile");
	}

	@Test
	void fallsBackToSubjectWhenPreferredUsernameIsMissing() {
		var authentication = converter.convert(jwt(Map.of()));

		assertThat(authentication.getName()).isEqualTo("user-1");
	}

	@Test
	void ignoresMalformedRoleClaims() {
		Jwt jwt = jwt(Map.of(
				"realm_access", Map.of("roles", "admin"),
				"resource_access", Map.of("elitebet-api", Map.of("roles", List.of("bettor", 123))),
				"scope", " "));

		var authentication = converter.convert(jwt);

		assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_BETTOR");
	}

	private Jwt jwt(Map<String, Object> claims) {
		return new Jwt("token-value", Instant.parse("2026-04-22T00:00:00Z"),
				Instant.parse("2026-04-22T01:00:00Z"), Map.of("alg", "none"),
				merge(Map.of("sub", "user-1"), claims));
	}

	private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> overrides) {
		java.util.HashMap<String, Object> merged = new java.util.HashMap<>(base);
		merged.putAll(overrides);
		return merged;
	}
}
