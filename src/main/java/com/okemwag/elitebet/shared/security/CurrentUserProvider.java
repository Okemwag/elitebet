package com.okemwag.elitebet.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public interface CurrentUserProvider {

	default CurrentUser currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new IllegalStateException("No authenticated user is available");
		}
		return SecurityUtils.currentUser(jwt, authentication.getAuthorities());
	}
}
