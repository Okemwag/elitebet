package com.okemwag.elitebet.shared.exception;

import java.time.Instant;

public record ApiError(ErrorCode code, String message, Instant timestamp) {
}
