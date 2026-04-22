package com.okemwag.elitebet.shared.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.okemwag.elitebet.shared.logging.MdcKeys;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<ApiError.FieldError> fieldErrors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toApiFieldError)
			.toList();
		return error(HttpStatus.BAD_REQUEST,
				ApiError.validation("Request validation failed", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(),
						correlationId(), fieldErrors));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST,
				ApiError.of(ErrorCode.VALIDATION_ERROR, "Request validation failed", HttpStatus.BAD_REQUEST.value(),
						request.getRequestURI(), correlationId()));
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ResponseEntity<ApiError> handleMissingRequestHeader(MissingRequestHeaderException exception,
			HttpServletRequest request) {
		String message = "Missing required header: " + exception.getHeaderName();
		return error(HttpStatus.BAD_REQUEST,
				ApiError.of(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI(),
						correlationId()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST,
				ApiError.of(ErrorCode.MALFORMED_REQUEST, "Malformed request body", HttpStatus.BAD_REQUEST.value(),
						request.getRequestURI(), correlationId()));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiError> handleNotFound(NotFoundException exception, HttpServletRequest request) {
		return businessError(HttpStatus.NOT_FOUND, exception, request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND,
				ApiError.of(ErrorCode.NOT_FOUND, "Resource not found", HttpStatus.NOT_FOUND.value(),
						request.getRequestURI(), correlationId()));
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
		return businessError(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	ResponseEntity<ApiError> handleForbiddenOperation(ForbiddenOperationException exception, HttpServletRequest request) {
		return businessError(HttpStatus.FORBIDDEN, exception, request);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException exception, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header("Retry-After", String.valueOf(Math.max(1, exception.retryAfter().toSeconds())))
			.body(ApiError.of(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getMessage(),
					HttpStatus.TOO_MANY_REQUESTS.value(), request.getRequestURI(), correlationId()));
	}

	@ExceptionHandler(ExternalIntegrationException.class)
	ResponseEntity<ApiError> handleExternalIntegration(ExternalIntegrationException exception, HttpServletRequest request) {
		return businessError(HttpStatus.BAD_GATEWAY, exception, request);
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiError> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		return businessError(HttpStatus.UNPROCESSABLE_ENTITY, exception, request);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED,
				ApiError.of(ErrorCode.UNAUTHORIZED, "Authentication required", HttpStatus.UNAUTHORIZED.value(),
						request.getRequestURI(), correlationId()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN,
				ApiError.of(ErrorCode.FORBIDDEN, "Access denied", HttpStatus.FORBIDDEN.value(), request.getRequestURI(),
						correlationId()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR,
				ApiError.of(ErrorCode.INTERNAL_ERROR, "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR.value(),
						request.getRequestURI(), correlationId()));
	}

	private ResponseEntity<ApiError> businessError(HttpStatus status, BusinessException exception,
			HttpServletRequest request) {
		return error(status,
				ApiError.of(exception.errorCode(), exception.getMessage(), status.value(), request.getRequestURI(),
						correlationId()));
	}

	private ResponseEntity<ApiError> error(HttpStatus status, ApiError apiError) {
		return ResponseEntity.status(status).body(apiError);
	}

	private ApiError.FieldError toApiFieldError(FieldError fieldError) {
		return new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage(),
				fieldError.getRejectedValue());
	}

	private String correlationId() {
		return MdcKeys.get(MdcKeys.CORRELATION_ID);
	}
}
