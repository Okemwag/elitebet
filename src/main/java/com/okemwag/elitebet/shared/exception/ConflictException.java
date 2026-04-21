package com.okemwag.elitebet.shared.exception;

public class ConflictException extends BusinessException {
	public ConflictException(String message) {
		super(ErrorCode.CONFLICT, message);
	}
}
