package com.okemwag.elitebet.wallet.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.shared.exception.BusinessException;
import com.okemwag.elitebet.shared.exception.ConflictException;
import com.okemwag.elitebet.shared.exception.NotFoundException;
import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotencyRecord;
import com.okemwag.elitebet.shared.idempotency.IdempotencyService;
import com.okemwag.elitebet.shared.idempotency.IdempotencyStatus;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.outbox.OutboxEvent;
import com.okemwag.elitebet.shared.outbox.OutboxEventType;
import com.okemwag.elitebet.shared.outbox.OutboxRepository;
import com.okemwag.elitebet.wallet.application.command.CaptureReservationCommand;
import com.okemwag.elitebet.wallet.application.command.CreateWalletCommand;
import com.okemwag.elitebet.wallet.application.command.CreditWalletCommand;
import com.okemwag.elitebet.wallet.application.command.ReleaseReservationCommand;
import com.okemwag.elitebet.wallet.application.command.ReserveFundsCommand;
import com.okemwag.elitebet.wallet.application.dto.WalletOperationResult;
import com.okemwag.elitebet.wallet.domain.enums.LedgerEntryType;
import com.okemwag.elitebet.wallet.domain.enums.TransactionType;
import com.okemwag.elitebet.wallet.domain.valueobject.Money;
import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.FundReservationEntity;
import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.WalletEntity;
import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.WalletTransactionEntity;
import com.okemwag.elitebet.wallet.infrastructure.persistence.repository.JpaFundReservationRepository;
import com.okemwag.elitebet.wallet.infrastructure.persistence.repository.JpaLedgerEntryRepository;
import com.okemwag.elitebet.wallet.infrastructure.persistence.repository.JpaWalletRepository;
import com.okemwag.elitebet.wallet.infrastructure.persistence.repository.JpaWalletTransactionRepository;

@Service
@ConditionalOnBean({ JpaWalletRepository.class, JpaWalletTransactionRepository.class, JpaFundReservationRepository.class,
		JpaLedgerEntryRepository.class })
public class DefaultWalletService implements WalletService {

	private static final String WALLET_AGGREGATE = "wallet";

	private final JpaWalletRepository walletRepository;

	private final JpaWalletTransactionRepository transactionRepository;

	private final JpaFundReservationRepository reservationRepository;

	private final JpaLedgerEntryRepository ledgerEntryRepository;

	private final IdempotencyService idempotencyService;

	private final OutboxRepository outboxRepository;

	private final ObjectMapper objectMapper;

	private final Clock clock;

	public DefaultWalletService(JpaWalletRepository walletRepository,
			JpaWalletTransactionRepository transactionRepository, JpaFundReservationRepository reservationRepository,
			JpaLedgerEntryRepository ledgerEntryRepository, IdempotencyService idempotencyService,
			OutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.reservationRepository = reservationRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.idempotencyService = idempotencyService;
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	@Transactional
	public WalletOperationResult createWallet(CreateWalletCommand command) {
		validateCreateWallet(command);
		String currencyCode = command.currency().value();
		WalletEntity wallet = walletRepository.findByPrincipalIdAndCurrencyCode(command.principalId(), currencyCode)
			.orElseGet(() -> {
				WalletEntity created = walletRepository.save(WalletEntity.create(command.principalId(), currencyCode, now()));
				publish(created, "wallet.created", Map.of("walletId", created.id(), "principalId", created.principalId(),
						"currencyCode", created.currencyCode()));
				return created;
			});
		return result(wallet, null, null);
	}

	@Override
	@Transactional
	public WalletOperationResult credit(CreditWalletCommand command) {
		validateCredit(command);
		IdempotencyRecord idempotency = start(command.operationType(), command.idempotencyKey(), command.actorId(),
				requestHash("credit", command.principalId(), command.amount(), command.transactionType(),
						command.referenceType(), command.referenceId(), command.externalReference()));
		if (idempotency.status() == IdempotencyStatus.COMPLETED) {
			return completedResult(idempotency);
		}
		try {
			WalletEntity wallet = walletRepository
				.findByPrincipalIdAndCurrencyCode(command.principalId(), command.amount().currency().getCurrencyCode())
				.orElseGet(() -> walletRepository.save(WalletEntity.create(command.principalId(),
						command.amount().currency().getCurrencyCode(), now())));
			WalletTransactionEntity transaction = transactionRepository.save(WalletTransactionEntity.pending(wallet.id(),
					command.transactionType(), command.amount().minorUnits(), wallet.currencyCode(),
					command.idempotencyKey().value(), command.referenceType(), command.referenceId(),
					command.externalReference(), "{}", now()));
			wallet.credit(command.amount().minorUnits(), now());
			transaction.post(now());
			ledgerEntryRepository.save(LedgerEntryEntity.create(wallet.id(), transaction.id(), null,
					LedgerEntryType.CREDIT, command.amount().minorUnits(), wallet.currencyCode(), wallet.balanceMinor(),
					command.referenceType(), command.referenceId(), "{}", now()));
			WalletOperationResult result = result(wallet, transaction.id(), null);
			publish(wallet, "wallet.credited", eventPayload(result, command.referenceType(), command.referenceId()));
			complete(idempotency, result);
			return result;
		}
		catch (RuntimeException exception) {
			fail(idempotency, exception);
			throw exception;
		}
	}

	@Override
	@Transactional
	public WalletOperationResult reserve(ReserveFundsCommand command) {
		validateReserve(command);
		IdempotencyRecord idempotency = start(IdempotentOperationType.BET_PLACEMENT, command.idempotencyKey(),
				command.actorId(), requestHash("reserve", command.principalId(), command.amount(), command.referenceType(),
						command.referenceId(), command.expiresAt()));
		if (idempotency.status() == IdempotencyStatus.COMPLETED) {
			return completedResult(idempotency);
		}
		try {
			WalletEntity wallet = walletRepository
				.findByPrincipalIdAndCurrencyCode(command.principalId(), command.amount().currency().getCurrencyCode())
				.orElseThrow(() -> new NotFoundException("Wallet not found"));
			wallet.reserve(command.amount().minorUnits(), now());
			WalletTransactionEntity transaction = transactionRepository.save(WalletTransactionEntity.pending(wallet.id(),
					TransactionType.BET_STAKE, command.amount().minorUnits(), wallet.currencyCode(),
					command.idempotencyKey().value(), command.referenceType(), command.referenceId(), null, "{}", now()));
			FundReservationEntity reservation = reservationRepository.save(FundReservationEntity.held(wallet.id(),
					transaction.id(), command.amount().minorUnits(), wallet.currencyCode(), command.idempotencyKey().value(),
					command.referenceType(), command.referenceId(), command.expiresAt(), now()));
			WalletOperationResult result = result(wallet, transaction.id(), reservation.id());
			publish(wallet, "wallet.reservation.created", eventPayload(result, command.referenceType(), command.referenceId()));
			complete(idempotency, result);
			return result;
		}
		catch (RuntimeException exception) {
			fail(idempotency, exception);
			throw exception;
		}
	}

	@Override
	@Transactional
	public WalletOperationResult captureReservation(CaptureReservationCommand command) {
		validateReservationCommand(command.reservationId(), command.idempotencyKey(), command.operationType(),
				command.actorId());
		IdempotencyRecord idempotency = start(command.operationType(), command.idempotencyKey(), command.actorId(),
				requestHash("capture", command.reservationId()));
		if (idempotency.status() == IdempotencyStatus.COMPLETED) {
			return completedResult(idempotency);
		}
		try {
			FundReservationEntity reservation = reservationRepository.findById(command.reservationId())
				.orElseThrow(() -> new NotFoundException("Fund reservation not found"));
			WalletEntity wallet = walletRepository.findById(reservation.walletId())
				.orElseThrow(() -> new NotFoundException("Wallet not found"));
			WalletTransactionEntity transaction = transactionRepository.findById(reservation.transactionId())
				.orElseThrow(() -> new NotFoundException("Wallet transaction not found"));
			wallet.captureReserved(reservation.amountMinor(), now());
			reservation.capture(now());
			transaction.post(now());
			ledgerEntryRepository.save(LedgerEntryEntity.create(wallet.id(), transaction.id(), reservation.id(),
					LedgerEntryType.DEBIT, reservation.amountMinor(), wallet.currencyCode(), wallet.balanceMinor(), "RESERVATION",
					reservation.id().toString(), "{}", now()));
			WalletOperationResult result = result(wallet, transaction.id(), reservation.id());
			publish(wallet, "wallet.reservation.captured", eventPayload(result, "RESERVATION", reservation.id().toString()));
			complete(idempotency, result);
			return result;
		}
		catch (RuntimeException exception) {
			fail(idempotency, exception);
			throw exception;
		}
	}

	@Override
	@Transactional
	public WalletOperationResult releaseReservation(ReleaseReservationCommand command) {
		validateReservationCommand(command.reservationId(), command.idempotencyKey(), command.operationType(),
				command.actorId());
		IdempotencyRecord idempotency = start(command.operationType(), command.idempotencyKey(), command.actorId(),
				requestHash("release", command.reservationId()));
		if (idempotency.status() == IdempotencyStatus.COMPLETED) {
			return completedResult(idempotency);
		}
		try {
			FundReservationEntity reservation = reservationRepository.findById(command.reservationId())
				.orElseThrow(() -> new NotFoundException("Fund reservation not found"));
			WalletEntity wallet = walletRepository.findById(reservation.walletId())
				.orElseThrow(() -> new NotFoundException("Wallet not found"));
			wallet.releaseReserved(reservation.amountMinor(), now());
			reservation.release(now());
			WalletOperationResult result = result(wallet, reservation.transactionId(), reservation.id());
			publish(wallet, "wallet.reservation.released", eventPayload(result, "RESERVATION", reservation.id().toString()));
			complete(idempotency, result);
			return result;
		}
		catch (RuntimeException exception) {
			fail(idempotency, exception);
			throw exception;
		}
	}

	private IdempotencyRecord start(IdempotentOperationType operationType, IdempotencyKey key, String actorId,
			String requestHash) {
		return idempotencyService.startOrGet(operationType, key, actorId, requestHash);
	}

	private void complete(IdempotencyRecord idempotency, WalletOperationResult result) {
		idempotencyService.complete(idempotency, 200, json(result));
	}

	private void fail(IdempotencyRecord idempotency, RuntimeException exception) {
		idempotencyService.fail(idempotency, exception.getMessage());
	}

	private WalletOperationResult completedResult(IdempotencyRecord record) {
		try {
			return objectMapper.readValue(record.responseBody(), WalletOperationResult.class);
		}
		catch (JsonProcessingException exception) {
			throw new ConflictException("Stored idempotent wallet response could not be read");
		}
	}

	private WalletOperationResult result(WalletEntity wallet, UUID transactionId, UUID reservationId) {
		return new WalletOperationResult(wallet.id(), transactionId, reservationId, wallet.currencyCode(),
				wallet.balanceMinor(), wallet.reservedMinor(), wallet.availableMinor());
	}

	private Map<String, Object> eventPayload(WalletOperationResult result, String referenceType, String referenceId) {
		return Map.of("walletId", result.walletId(), "transactionId", nullable(result.transactionId()), "reservationId",
				nullable(result.reservationId()), "currencyCode", result.currencyCode(), "balanceMinor",
				result.balanceMinor(), "reservedMinor", result.reservedMinor(), "availableMinor", result.availableMinor(),
				"referenceType", nullable(referenceType), "referenceId", nullable(referenceId));
	}

	private void publish(WalletEntity wallet, String eventName, Map<String, Object> payload) {
		Instant now = now();
		outboxRepository.save(OutboxEvent.create(OutboxEventType.INTEGRATION_EVENT, WALLET_AGGREGATE,
				wallet.id().toString(), eventName, json(payload), json(Map.of("correlationId",
						nullable(MdcKeys.get(MdcKeys.CORRELATION_ID)), "actorId", nullable(MdcKeys.get(MdcKeys.USER_ID)))),
				now, now));
	}

	private String requestHash(Object... values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(json(values).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new BusinessException("SHA-256 is not available for wallet idempotency");
		}
	}

	private String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException exception) {
			throw new BusinessException("Wallet event payload could not be serialized");
		}
	}

	private String nullable(Object value) {
		return value == null ? "" : value.toString();
	}

	private Instant now() {
		return clock.instant();
	}

	private void validateCreateWallet(CreateWalletCommand command) {
		Objects.requireNonNull(command, "command is required");
		requireText(command.principalId(), "principalId");
		Objects.requireNonNull(command.currency(), "currency is required");
	}

	private void validateCredit(CreditWalletCommand command) {
		Objects.requireNonNull(command, "command is required");
		requireText(command.principalId(), "principalId");
		requireAmount(command.amount());
		Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey is required");
		Objects.requireNonNull(command.operationType(), "operationType is required");
		Objects.requireNonNull(command.transactionType(), "transactionType is required");
		requireText(command.actorId(), "actorId");
		if (command.transactionType() == TransactionType.BET_STAKE) {
			throw new IllegalArgumentException("credit transaction cannot be BET_STAKE");
		}
	}

	private void validateReserve(ReserveFundsCommand command) {
		Objects.requireNonNull(command, "command is required");
		requireText(command.principalId(), "principalId");
		requireAmount(command.amount());
		Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey is required");
		requireText(command.referenceType(), "referenceType");
		requireText(command.referenceId(), "referenceId");
		requireText(command.actorId(), "actorId");
		if (command.expiresAt() == null || !command.expiresAt().isAfter(now())) {
			throw new IllegalArgumentException("reservation expiry must be in the future");
		}
	}

	private void validateReservationCommand(UUID reservationId, IdempotencyKey idempotencyKey,
			IdempotentOperationType operationType, String actorId) {
		Objects.requireNonNull(reservationId, "reservationId is required");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
		Objects.requireNonNull(operationType, "operationType is required");
		requireText(actorId, "actorId");
	}

	private void requireAmount(Money amount) {
		Objects.requireNonNull(amount, "amount is required").requirePositive("amount");
	}

	private void requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
	}
}
