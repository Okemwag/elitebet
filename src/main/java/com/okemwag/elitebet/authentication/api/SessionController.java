package com.okemwag.elitebet.authentication.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okemwag.elitebet.authentication.api.request.AccountLockRequest;
import com.okemwag.elitebet.authentication.api.request.AdminReasonRequest;
import com.okemwag.elitebet.authentication.application.AccountAccessService;
import com.okemwag.elitebet.authentication.application.AuthAuditService;
import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.response.ApiResponse;

@Validated
@RestController
@RequestMapping("/api/v1/admin/auth/accounts")
public class SessionController {

	private final AccountAccessService accountAccessService;

	private final AuthAuditService authAuditService;

	public SessionController(AccountAccessService accountAccessService, AuthAuditService authAuditService) {
		this.accountAccessService = accountAccessService;
		this.authAuditService = authAuditService;
	}

	@PutMapping("/{principalId}/lock")
	@PreAuthorize("hasAnyRole('ADMIN','SUPPORT','COMPLIANCE_OFFICER')")
	public ApiResponse<AuthAccountView> lock(@PathVariable String principalId,
			@Valid @RequestBody AccountLockRequest request,
			HttpServletRequest httpRequest) {
		AuthAccountView account = accountAccessService.lock(principalId, request.lockedUntil());
		authAuditService.accountLocked(principalId, request.reason(), httpRequest);
		return ApiResponse.ok(account, MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PutMapping("/{principalId}/unlock")
	@PreAuthorize("hasAnyRole('ADMIN','SUPPORT','COMPLIANCE_OFFICER')")
	public ApiResponse<AuthAccountView> unlock(@PathVariable String principalId,
			@Valid @RequestBody AdminReasonRequest request,
			HttpServletRequest httpRequest) {
		AuthAccountView account = accountAccessService.unlock(principalId);
		authAuditService.accountUnlocked(principalId, request.reason(), httpRequest);
		return ApiResponse.ok(account, MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PostMapping("/{principalId}/disable")
	@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
	public ResponseEntity<ApiResponse<AuthAccountView>> disable(@PathVariable String principalId,
			@Valid @RequestBody AdminReasonRequest request,
			HttpServletRequest httpRequest) {
		AuthAccountView account = accountAccessService.disable(principalId);
		authAuditService.accountDisabled(principalId, request.reason(), httpRequest);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(ApiResponse.accepted(account, "Account disabled", MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}
}
