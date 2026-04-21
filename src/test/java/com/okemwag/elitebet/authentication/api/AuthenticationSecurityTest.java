package com.okemwag.elitebet.authentication.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okemwag.elitebet.authentication.api.request.AdminReasonRequest;
import com.okemwag.elitebet.authentication.api.request.RegisterAccountRequest;
import com.okemwag.elitebet.authentication.application.AccountAccessService;
import com.okemwag.elitebet.authentication.application.SessionService;
import com.okemwag.elitebet.authentication.application.RegistrationService;
import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.authentication.domain.enums.AccountStatus;
import com.okemwag.elitebet.authentication.domain.enums.MfaStatus;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.shared.security.RoleConstants;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
})
@AutoConfigureMockMvc
class AuthenticationSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthAccountRepository authAccountRepository;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private AccountAccessService accountAccessService;

	@MockitoBean
	private SessionService sessionService;

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsJwtPrincipalAndRoles() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(jwt -> jwt.subject("user-1")
			.claim("preferred_username", "bettor1")
			.claim("email", "bettor@example.com")
			.claim("email_verified", true)
			.claim("realm_access", java.util.Map.of("roles", java.util.List.of("bettor"))))
			.authorities(new SimpleGrantedAuthority("ROLE_BETTOR"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.principalId").value("user-1"))
			.andExpect(jsonPath("$.data.username").value("bettor1"))
			.andExpect(jsonPath("$.data.email").value("bettor@example.com"))
			.andExpect(jsonPath("$.data.emailVerified").value(true))
			.andExpect(jsonPath("$.data.roles[0]").value(RoleConstants.BETTOR));
	}

	@Test
	void registerIsPublicButValidated() throws Exception {
		RegisterAccountRequest invalid = new RegisterAccountRequest("", "not-email", "short", false, false);

		mockMvc.perform(post("/api/v1/auth/register")
			.header("Idempotency-Key", "registration-key-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(invalid)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void registerCreatesAccountWithoutCallerJwt() throws Exception {
		RegisterAccountRequest request = new RegisterAccountRequest("bettor1", "bettor@example.com",
				"correct-horse-password", true, false);
		when(registrationService.register(any(RegisterAccountRequest.class), any()))
			.thenReturn(new AuthAccountView("user-1", "bettor1", "bettor@example.com", false,
					AccountStatus.PENDING_ACTIVATION, MfaStatus.NOT_CONFIGURED, Set.of(RoleConstants.BETTOR), null, null));

		mockMvc.perform(post("/api/v1/auth/register")
			.header("Idempotency-Key", "registration-key-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.principalId").value("user-1"))
			.andExpect(jsonPath("$.data.status").value(AccountStatus.PENDING_ACTIVATION.name()));
	}

	@Test
	void registerRequiresIdempotencyKey() throws Exception {
		RegisterAccountRequest request = new RegisterAccountRequest("bettor1", "bettor@example.com",
				"correct-horse-password", true, false);

		mockMvc.perform(post("/api/v1/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void logoutRequiresAuthenticationAndRevokesRefreshToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"refreshToken":"refresh-token-1"}
					"""))
			.andExpect(status().isAccepted());
	}

	@Test
	void bettorCannotLockAccounts() throws Exception {
		mockMvc.perform(put("/api/v1/admin/auth/accounts/user-1/lock")
			.with(jwt()
				.jwt(jwt -> jwt.claim("realm_access", java.util.Map.of("roles", java.util.List.of("bettor"))))
				.authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"lockedUntil":"2026-04-23T00:00:00Z","reason":"risk review"}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanDisableAccount() throws Exception {
		when(accountAccessService.disable(eq("user-1"))).thenReturn(new AuthAccountView("user-1", "bettor1",
				"bettor@example.com", true, AccountStatus.DISABLED, MfaStatus.NOT_CONFIGURED, Set.of(), null,
				Instant.parse("2026-04-22T00:00:00Z")));

		mockMvc.perform(post("/api/v1/admin/auth/accounts/user-1/disable")
			.with(jwt()
				.jwt(jwt -> jwt.claim("realm_access", java.util.Map.of("roles", java.util.List.of("admin"))))
				.authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new AdminReasonRequest("terms violation"))))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.data.status").value(AccountStatus.DISABLED.name()));
	}
}
