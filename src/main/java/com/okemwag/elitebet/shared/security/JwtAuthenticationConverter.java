package com.okemwag.elitebet.shared.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

	@Override
	public JwtAuthenticationToken convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new HashSet<>();
		addRealmRoles(jwt, authorities);
		addResourceRoles(jwt, authorities);
		addScopes(jwt, authorities);
		String principalName = jwt.getClaimAsString("preferred_username");
		if (principalName == null || principalName.isBlank()) {
			principalName = jwt.getSubject();
		}
		return new JwtAuthenticationToken(jwt, authorities, principalName);
	}

	private void addRealmRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess != null) {
			addRoles(realmAccess.get("roles"), authorities);
		}
	}

	private void addResourceRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
		Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess == null) {
			return;
		}
		resourceAccess.values().stream().filter(Map.class::isInstance).map(Map.class::cast).forEach(resource -> addRoles(resource.get("roles"), authorities));
	}

	private void addScopes(Jwt jwt, Set<GrantedAuthority> authorities) {
		String scope = jwt.getClaimAsString("scope");
		if (scope == null || scope.isBlank()) {
			return;
		}
		for (String value : scope.split(" ")) {
			if (!value.isBlank()) {
				authorities.add(new SimpleGrantedAuthority("SCOPE_" + value));
			}
		}
	}

	private void addRoles(Object roles, Set<GrantedAuthority> authorities) {
		if (!(roles instanceof Collection<?> roleValues)) {
			return;
		}
		roleValues.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.map(String::trim)
			.filter(role -> !role.isBlank())
			.map(String::toUpperCase)
			.map(role -> RoleConstants.ROLE_PREFIX + role)
			.map(SimpleGrantedAuthority::new)
			.forEach(authorities::add);
	}
}
