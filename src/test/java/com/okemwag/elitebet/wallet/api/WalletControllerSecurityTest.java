package com.okemwag.elitebet.wallet.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
import com.okemwag.elitebet.user.application.UserProfileAuditService;
import com.okemwag.elitebet.user.application.UserProfileService;
import com.okemwag.elitebet.wallet.api.request.AdminCreditWalletRequest;
import com.okemwag.elitebet.wallet.api.request.CreateWalletRequest;
import com.okemwag.elitebet.wallet.api.request.ReserveFundsRequest;
import com.okemwag.elitebet.wallet.application.WalletService;
import com.okemwag.elitebet.wallet.application.dto.WalletOperationResult;
import com.okemwag.elitebet.wallet.domain.enums.TransactionType;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
})
@AutoConfigureMockMvc
class WalletControllerSecurityTest {

	private static final UUID WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private static final UUID TRANSACTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private static final UUID RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthAccountRepository authAccountRepository;

	@MockitoBean
	private WalletService walletService;

	@MockitoBean
	private UserProfileService userProfileService;

	@MockitoBean
	private UserProfileAuditService userProfileAuditService;

	@Test
	void walletEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/wallet")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new CreateWalletRequest("KES"))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void bettorCanCreateWalletForSelf() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));
		when(walletService.createWallet(any())).thenReturn(result(null, null, 0, 0));

		mockMvc.perform(post("/api/v1/wallet")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new CreateWalletRequest("KES"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.walletId").value(WALLET_ID.toString()))
			.andExpect(jsonPath("$.data.currencyCode").value("KES"));
	}

	@Test
	void reserveRequiresIdempotencyKey() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));

		mockMvc.perform(post("/api/v1/wallet/reservations")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new ReserveFundsRequest(new java.math.BigDecimal("10.00"), "KES",
					"BET", "bet-1", Instant.parse("2026-04-22T01:00:00Z")))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void bettorCanReserveFundsForSelf() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));
		when(walletService.reserve(any())).thenReturn(result(TRANSACTION_ID, RESERVATION_ID, 10_000, 2_500));

		mockMvc.perform(post("/api/v1/wallet/reservations")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.header("Idempotency-Key", "reserve-key-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new ReserveFundsRequest(new java.math.BigDecimal("25.00"), "KES",
					"BET", "bet-1", Instant.parse("2026-04-22T01:00:00Z")))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.reservationId").value(RESERVATION_ID.toString()))
			.andExpect(jsonPath("$.data.availableMinor").value(7_500));
	}

	@Test
	void adminCreditRequiresWalletAdjustPermission() throws Exception {
		when(authAccountRepository.findByPrincipalId("user-1")).thenReturn(Optional.of(activeAccount("user-1")));

		mockMvc.perform(post("/api/v1/admin/wallet/user-2/credits")
			.with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_BETTOR")))
			.header("Idempotency-Key", "credit-key-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new AdminCreditWalletRequest(new java.math.BigDecimal("100.00"),
					"KES", TransactionType.ADJUSTMENT, "ADMIN", "case-1", null, "test credit"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCreditWallet() throws Exception {
		when(authAccountRepository.findByPrincipalId("admin-1")).thenReturn(Optional.of(activeAccount("admin-1")));
		when(walletService.credit(any())).thenReturn(result(TRANSACTION_ID, null, 10_000, 0));

		mockMvc.perform(post("/api/v1/admin/wallet/user-2/credits")
			.with(jwt().jwt(jwt -> jwt.subject("admin-1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
			.header("Idempotency-Key", "credit-key-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new AdminCreditWalletRequest(new java.math.BigDecimal("100.00"),
					"KES", TransactionType.ADJUSTMENT, "ADMIN", "case-1", null, "test credit"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.transactionId").value(TRANSACTION_ID.toString()))
			.andExpect(jsonPath("$.data.balanceMinor").value(10_000));
	}

	private WalletOperationResult result(UUID transactionId, UUID reservationId, long balanceMinor, long reservedMinor) {
		return new WalletOperationResult(WALLET_ID, transactionId, reservationId, "KES", balanceMinor, reservedMinor,
				balanceMinor - reservedMinor);
	}

	private AuthAccount activeAccount(String principalId) {
		return AuthAccount.create(principalId, "EB100000000000", principalId, principalId + "@example.com",
				AuthProvider.KEYCLOAK, principalId, true, Instant.parse("2026-04-22T00:00:00Z"));
	}
}
