package com.okemwag.elitebet.authentication.infrastructure.client;

public record KeycloakUserRegistration(
		String username,
		String email,
		String password,
		boolean emailVerified) {
}
