package com.okemwag.elitebet.shared.security;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RolePermissionPolicy {

	private static final Set<String> ALL_PERMISSIONS = Set.of(PermissionConstants.ADMIN_READ,
			PermissionConstants.ADMIN_WRITE, PermissionConstants.AUTH_ACCOUNT_LOCK,
			PermissionConstants.AUTH_ACCOUNT_UNLOCK, PermissionConstants.AUTH_ACCOUNT_DISABLE,
			PermissionConstants.AUTH_SESSION_REVOKE, PermissionConstants.WALLET_ADJUST,
			PermissionConstants.MARKET_CONTROL, PermissionConstants.SETTLEMENT_WRITE,
			PermissionConstants.KYC_REVIEW, PermissionConstants.RISK_REVIEW);

	private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
			RoleConstants.ADMIN, ALL_PERMISSIONS,
			RoleConstants.COMPLIANCE_OFFICER, Set.of(PermissionConstants.ADMIN_READ,
					PermissionConstants.AUTH_ACCOUNT_LOCK, PermissionConstants.AUTH_ACCOUNT_UNLOCK,
					PermissionConstants.AUTH_ACCOUNT_DISABLE, PermissionConstants.AUTH_SESSION_REVOKE,
					PermissionConstants.KYC_REVIEW, PermissionConstants.RISK_REVIEW),
			RoleConstants.SUPPORT, Set.of(PermissionConstants.ADMIN_READ, PermissionConstants.AUTH_ACCOUNT_LOCK,
					PermissionConstants.AUTH_ACCOUNT_UNLOCK, PermissionConstants.AUTH_SESSION_REVOKE),
			RoleConstants.OPERATOR, Set.of(PermissionConstants.ADMIN_READ, PermissionConstants.MARKET_CONTROL,
					PermissionConstants.SETTLEMENT_WRITE),
			RoleConstants.RISK_ANALYST, Set.of(PermissionConstants.ADMIN_READ, PermissionConstants.RISK_REVIEW),
			RoleConstants.BETTOR, Set.of());

	public boolean grants(String role, String permission) {
		return ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission);
	}

	public Set<String> permissionsFor(Set<String> roles) {
		if (roles.isEmpty()) {
			return Set.of();
		}
		Set<String> permissions = new java.util.HashSet<>();
		roles.forEach(role -> permissions.addAll(ROLE_PERMISSIONS.getOrDefault(role, Set.of())));
		if (permissions.isEmpty()) {
			return Set.of();
		}
		return Set.copyOf(permissions);
	}
}
