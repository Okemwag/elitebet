package com.okemwag.elitebet.authentication.infrastructure.client;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "elitebet.keycloak")
public record KeycloakProperties(
		@NotBlank String baseUrl,
		@NotBlank String realm,
		@NotBlank String adminClientId,
		@NotBlank String adminClientSecret) {
}
