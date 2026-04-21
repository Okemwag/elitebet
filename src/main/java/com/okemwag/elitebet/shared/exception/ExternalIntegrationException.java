package com.okemwag.elitebet.shared.exception;

public class ExternalIntegrationException extends BusinessException {
	public ExternalIntegrationException(String message) {
		super(ErrorCode.EXTERNAL_INTEGRATION_ERROR, message);
	}
}
