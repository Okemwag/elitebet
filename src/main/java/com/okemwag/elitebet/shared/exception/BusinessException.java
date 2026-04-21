package com.okemwag.elitebet.shared.exception;

public class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;

	public BusinessException(String message) {
		this(ErrorCode.BUSINESS_RULE_VIOLATION, message);
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}
