package com.okemwag.elitebet.shared.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String message, String correlationId, Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data, String correlationId) {
		return new ApiResponse<>(data, null, correlationId, Instant.now());
	}

	public static <T> ApiResponse<T> accepted(T data, String message, String correlationId) {
		return new ApiResponse<>(data, message, correlationId, Instant.now());
	}
}
