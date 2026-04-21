package com.okemwag.elitebet.shared.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("authorizationService")
public class AuthorizationService {

	private final RolePermissionPolicy rolePermissionPolicy;

	public AuthorizationService(RolePermissionPolicy rolePermissionPolicy) {
		this.rolePermissionPolicy = rolePermissionPolicy;
	}

	public boolean hasPermission(Authentication authentication, String permission) {
		if (authentication == null || !authentication.isAuthenticated() || permission == null || permission.isBlank()) {
			return false;
		}
		return roleNames(authentication.getAuthorities()).stream()
			.anyMatch(role -> rolePermissionPolicy.grants(role, permission));
	}

	private Collection<String> roleNames(Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority.startsWith(RoleConstants.ROLE_PREFIX))
			.map(authority -> authority.substring(RoleConstants.ROLE_PREFIX.length()))
			.toList();
	}
}
