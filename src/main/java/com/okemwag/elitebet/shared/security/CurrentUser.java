package com.okemwag.elitebet.shared.security;

import java.util.Set;

public record CurrentUser(String principalId, String username, String email, Set<String> roles) {

	public boolean hasRole(String role) {
		return roles.contains(role);
	}
}
