package com.okemwag.elitebet.shared.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
		ErrorCode code,
		String message,
		int status,
		String path,
		String correlationId,
		List<FieldError> fieldErrors,
		Instant timestamp) {

	public static ApiError of(ErrorCode code, String message, int status, String path, String correlationId) {
		return new ApiError(code, message, status, path, correlationId, List.of(), Instant.now());
	}

	public static ApiError validation(String message, int status, String path, String correlationId,
			List<FieldError> fieldErrors) {
		return new ApiError(ErrorCode.VALIDATION_ERROR, message, status, path, correlationId, fieldErrors, Instant.now());
	}

	public record FieldError(String field, String message, Object rejectedValue) {
	}
}
