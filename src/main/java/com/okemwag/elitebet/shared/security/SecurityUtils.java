package com.okemwag.elitebet.shared.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {
	public static CurrentUser currentUser(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
		Set<String> roles = authorities.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority.startsWith(RoleConstants.ROLE_PREFIX))
			.map(authority -> authority.substring(RoleConstants.ROLE_PREFIX.length()))
			.collect(Collectors.toUnmodifiableSet());

		return new CurrentUser(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("email"),
				roles);
	}

	public static String currentPrincipalIdOrAnonymous() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "anonymous";
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return authentication.getName();
	}

	private SecurityUtils() {
	}
}
