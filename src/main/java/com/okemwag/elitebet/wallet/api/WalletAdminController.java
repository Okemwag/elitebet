package com.okemwag.elitebet.wallet.api;

import java.util.Currency;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.response.ApiResponse;
import com.okemwag.elitebet.wallet.api.request.AdminCreditWalletRequest;
import com.okemwag.elitebet.wallet.api.response.WalletOperationResponse;
import com.okemwag.elitebet.wallet.application.WalletService;
import com.okemwag.elitebet.wallet.application.command.CaptureReservationCommand;
import com.okemwag.elitebet.wallet.application.command.CreditWalletCommand;
import com.okemwag.elitebet.wallet.application.command.ReleaseReservationCommand;
import com.okemwag.elitebet.wallet.domain.valueobject.CurrencyCode;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;

@Validated
@RestController
@RequestMapping("/api/v1/admin/wallet")
public class WalletAdminController {

	private final WalletService walletService;

	public WalletAdminController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping("/{principalId}/credits")
	@PreAuthorize("@authorizationService.hasPermission(authentication, 'wallet:adjust')")
	public ApiResponse<WalletOperationResponse> credit(
			@PathVariable String principalId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			Authentication authentication,
			@Valid @RequestBody AdminCreditWalletRequest request) {
		Currency currency = new CurrencyCode(request.currencyCode()).toCurrency();
		WalletOperationResponse response = WalletOperationResponse.from(walletService.credit(new CreditWalletCommand(
				principalId, Money.ofMajor(request.amount(), currency), new IdempotencyKey(idempotencyKey),
				IdempotentOperationType.WALLET_ADJUSTMENT, request.transactionType(), request.referenceType(),
				request.referenceId(), request.externalReference(), principalId(authentication))));
		return ApiResponse.ok(response, MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PostMapping("/reservations/{reservationId}/capture")
	@PreAuthorize("@authorizationService.hasPermission(authentication, 'settlement:write')")
	public ApiResponse<WalletOperationResponse> captureReservation(
			@PathVariable UUID reservationId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			Authentication authentication) {
		WalletOperationResponse response = WalletOperationResponse.from(walletService.captureReservation(
				new CaptureReservationCommand(reservationId, new IdempotencyKey(idempotencyKey),
						IdempotentOperationType.SETTLEMENT_REPLAY, principalId(authentication))));
		return ApiResponse.ok(response, MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PostMapping("/reservations/{reservationId}/release")
	@PreAuthorize("@authorizationService.hasPermission(authentication, 'wallet:adjust') "
			+ "or @authorizationService.hasPermission(authentication, 'settlement:write')")
	public ApiResponse<WalletOperationResponse> releaseReservation(
			@PathVariable UUID reservationId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			Authentication authentication) {
		WalletOperationResponse response = WalletOperationResponse.from(walletService.releaseReservation(
				new ReleaseReservationCommand(reservationId, new IdempotencyKey(idempotencyKey),
						IdempotentOperationType.SETTLEMENT_REPLAY, principalId(authentication))));
		return ApiResponse.ok(response, MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	private String principalId(Authentication authentication) {
		return ((Jwt) authentication.getPrincipal()).getSubject();
	}
}
