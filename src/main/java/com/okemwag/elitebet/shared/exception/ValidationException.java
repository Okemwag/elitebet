package com.okemwag.elitebet.shared.exception;

public class ValidationException extends BusinessException {
	public ValidationException(String message) {
		super(ErrorCode.VALIDATION_ERROR, message);
	}
}
