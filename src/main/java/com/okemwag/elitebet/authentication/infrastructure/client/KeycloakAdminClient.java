package com.okemwag.elitebet.authentication.infrastructure.client;

public interface KeycloakAdminClient {

	String createUser(KeycloakUserRegistration registration);

	void assignRealmRole(String providerUserId, String role);

	void setUserEnabled(String providerUserId, boolean enabled);

	void logoutUserSessions(String providerUserId);
}
