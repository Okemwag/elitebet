package com.okemwag.elitebet.wallet.api;

import java.util.Currency;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.response.ApiResponse;
import com.okemwag.elitebet.wallet.api.request.CreateWalletRequest;
import com.okemwag.elitebet.wallet.api.request.ReserveFundsRequest;
import com.okemwag.elitebet.wallet.api.response.WalletOperationResponse;
import com.okemwag.elitebet.wallet.application.WalletService;
import com.okemwag.elitebet.wallet.application.command.CreateWalletCommand;
import com.okemwag.elitebet.wallet.application.command.ReserveFundsCommand;
import com.okemwag.elitebet.wallet.domain.valueobject.CurrencyCode;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;

@Validated
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<WalletOperationResponse>> create(Authentication authentication,
			@Valid @RequestBody CreateWalletRequest request) {
		WalletOperationResponse response = WalletOperationResponse.from(walletService.createWallet(
				new CreateWalletCommand(principalId(authentication), new CurrencyCode(request.currencyCode()))));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(response, MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}

	@PostMapping("/reservations")
	public ResponseEntity<ApiResponse<WalletOperationResponse>> reserve(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			Authentication authentication,
			@Valid @RequestBody ReserveFundsRequest request) {
		Currency currency = new CurrencyCode(request.currencyCode()).toCurrency();
		WalletOperationResponse response = WalletOperationResponse.from(walletService.reserve(new ReserveFundsCommand(
				principalId(authentication), Money.ofMajor(request.amount(), currency), new IdempotencyKey(idempotencyKey),
				request.referenceType(), request.referenceId(), request.expiresAt(), principalId(authentication))));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(response, MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}

	private String principalId(Authentication authentication) {
		return ((Jwt) authentication.getPrincipal()).getSubject();
	}
}
