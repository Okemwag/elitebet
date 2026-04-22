package com.okemwag.elitebet.user.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okemwag.elitebet.authentication.domain.enums.AuthProvider;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.user.api.request.UpsertUserProfileRequest;
import com.okemwag.elitebet.user.application.UserProfileAuditService;
import com.okemwag.elitebet.user.application.UserProfileService;
import com.okemwag.elitebet.user.application.dto.UserProfileView;
import com.okemwag.elitebet.user.domain.enums.UserProfileStatus;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
})
@AutoConfigureMockMvc
class UserProfileSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthAccountRepository authAccountRepository;

	@MockitoBean
	private UserProfileService userProfileService;

	@MockitoBean
	private UserProfileAuditService auditService;

	@Test
	void profileRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/user/profile")).andExpect(status().isUnauthorized());
	}

	@Test
	void getReturnsCurrentUsersProfile() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));
		when(userProfileService.get("user-1")).thenReturn(profileView("user-1"));

		mockMvc.perform(get("/api/v1/user/profile")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.principalId").value("user-1"))
			.andExpect(jsonPath("$.data.countryCode").value("US"));
	}

	@Test
	void putCreatesCurrentUsersProfile() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));
		when(userProfileService.upsert(eq("user-1"), any())).thenReturn(
				new UserProfileService.UpsertResult(profileView("user-1"), true));

		mockMvc.perform(put("/api/v1/user/profile")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new UpsertUserProfileRequest("Ada", "Lovelace",
					LocalDate.parse("1990-01-01"), "US", "NY", "+15551234567"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.status").value(UserProfileStatus.COMPLETE.name()));
	}

	@Test
	void putValidatesProfileRequest() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));

		mockMvc.perform(put("/api/v1/user/profile")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName":"","lastName":"","dateOfBirth":"2030-01-01","countryCode":"USA","phoneNumber":"555"}
					"""))
			.andExpect(status().isBadRequest());
	}

	private UserProfileView profileView(String principalId) {
		return new UserProfileView(principalId, "Ada", "Lovelace", LocalDate.parse("1990-01-01"), "US", "NY",
				"+15551234567", UserProfileStatus.COMPLETE);
	}

	private AuthAccount activeAccount(String principalId) {
		return AuthAccount.create(principalId, "EB100000000000", principalId, principalId + "@example.com",
				AuthProvider.KEYCLOAK, principalId, true, Instant.parse("2026-04-22T00:00:00Z"));
	}
}
