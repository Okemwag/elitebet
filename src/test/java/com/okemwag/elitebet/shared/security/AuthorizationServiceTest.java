package com.okemwag.elitebet.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthorizationServiceTest {

	private final AuthorizationService authorizationService = new AuthorizationService(new RolePermissionPolicy());

	@Test
	void adminReceivesHighPrivilegePermissions() {
		Authentication authentication = authenticationWithRole(RoleConstants.ADMIN);

		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.AUTH_ACCOUNT_DISABLE)).isTrue();
		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.WALLET_ADJUST)).isTrue();
		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.SETTLEMENT_WRITE)).isTrue();
	}

	@Test
	void supportCanLockAndUnlockButCannotDisableAccounts() {
		Authentication authentication = authenticationWithRole(RoleConstants.SUPPORT);

		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.AUTH_ACCOUNT_LOCK)).isTrue();
		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.AUTH_ACCOUNT_UNLOCK)).isTrue();
		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.AUTH_ACCOUNT_DISABLE)).isFalse();
	}

	@Test
	void riskAnalystHasRiskReviewButNoAuthAdministration() {
		Authentication authentication = authenticationWithRole(RoleConstants.RISK_ANALYST);

		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.RISK_REVIEW)).isTrue();
		assertThat(authorizationService.hasPermission(authentication, PermissionConstants.AUTH_ACCOUNT_LOCK)).isFalse();
	}

	@Test
	void anonymousAuthenticationDoesNotGrantPermissions() {
		assertThat(authorizationService.hasPermission(null, PermissionConstants.ADMIN_READ)).isFalse();
		assertThat(authorizationService.hasPermission(authenticationWithRole(RoleConstants.ADMIN), "")).isFalse();
	}

	private Authentication authenticationWithRole(String role) {
		return UsernamePasswordAuthenticationToken.authenticated("principal", "n/a",
				List.of(new SimpleGrantedAuthority(RoleConstants.ROLE_PREFIX + role)));
	}
}
