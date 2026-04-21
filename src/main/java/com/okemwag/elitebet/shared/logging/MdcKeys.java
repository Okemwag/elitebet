package com.okemwag.elitebet.shared.logging;

import org.slf4j.MDC;

public final class MdcKeys {
	public static final String CORRELATION_ID = "correlationId";
	public static final String USER_ID = "userId";

	public static String get(String key) {
		return MDC.get(key);
	}

	private MdcKeys() {
	}
}
