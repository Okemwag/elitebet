package com.okemwag.elitebet.authentication.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okemwag.elitebet.authentication.api.request.LogoutRequest;
import com.okemwag.elitebet.authentication.api.request.RegisterAccountRequest;
import com.okemwag.elitebet.authentication.application.AuthAuditService;
import com.okemwag.elitebet.authentication.application.AuthenticationRateLimitService;
import com.okemwag.elitebet.authentication.application.RegistrationService;
import com.okemwag.elitebet.authentication.application.SessionService;
import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.authentication.application.dto.AuthSessionView;
import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.response.ApiResponse;
import com.okemwag.elitebet.shared.security.SecurityUtils;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

	private final RegistrationService registrationService;

	private final AuthenticationRateLimitService rateLimitService;

	private final SessionService sessionService;

	private final AuthAuditService authAuditService;

	public AuthenticationController(RegistrationService registrationService,
			AuthenticationRateLimitService rateLimitService,
			SessionService sessionService,
			AuthAuditService authAuditService) {
		this.registrationService = registrationService;
		this.rateLimitService = rateLimitService;
		this.sessionService = sessionService;
		this.authAuditService = authAuditService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthAccountView>> register(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody RegisterAccountRequest request,
			HttpServletRequest httpRequest) {
		rateLimitService.enforceRegistration(request, httpRequest);
		AuthAccountView account = registrationService.register(request, new IdempotencyKey(idempotencyKey));
		authAuditService.registrationCreated(account.principalId(), account.email(), httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(account, MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}

	@GetMapping("/me")
	public ApiResponse<AuthAccountView> me(Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		return ApiResponse.ok(new AuthAccountView(jwt.getSubject(), null, jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("email"), Boolean.TRUE.equals(jwt.getClaim("email_verified")), null, null,
				SecurityUtils.currentUser(jwt, authentication.getAuthorities()).roles(), null, null),
				MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@GetMapping("/session")
	public ApiResponse<AuthSessionView> session(Authentication authentication) {
		return ApiResponse.ok(sessionService.currentSession(authentication), MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication, @Valid @RequestBody LogoutRequest request,
			HttpServletRequest httpRequest) {
		sessionService.logout(request.refreshToken());
		authAuditService.logout(authentication.getName(), httpRequest);
		return ResponseEntity.accepted()
			.body(ApiResponse.accepted(null, "Logout accepted", MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}
}
