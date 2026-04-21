package com.okemwag.elitebet.authentication.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(@NotBlank @Size(max = 4096) String refreshToken) {
}
