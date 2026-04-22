package com.okemwag.elitebet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.okemwag.elitebet.shared.idempotency.IdempotencyKey;
import com.okemwag.elitebet.shared.idempotency.IdempotencyRecord;
import com.okemwag.elitebet.shared.idempotency.IdempotencyService;
import com.okemwag.elitebet.shared.idempotency.IdempotentOperationType;
import com.okemwag.elitebet.shared.outbox.OutboxEvent;
import com.okemwag.elitebet.shared.outbox.OutboxRepository;
import com.okemwag.elitebet.wallet.application.command.CaptureReservationCommand;
import com.okemwag.elitebet.wallet.application.command.CreditWalletCommand;
import com.okemwag.elitebet.wallet.application.command.ReserveFundsCommand;
import com.okemwag.elitebet.wallet.application.dto.WalletOperationResult;
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

class DefaultWalletServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-22T00:00:00Z");

	private static final Currency KES = Currency.getInstance("KES");

	private final JpaWalletRepository walletRepository = mock(JpaWalletRepository.class);

	private final JpaWalletTransactionRepository transactionRepository = mock(JpaWalletTransactionRepository.class);

	private final JpaFundReservationRepository reservationRepository = mock(JpaFundReservationRepository.class);

	private final JpaLedgerEntryRepository ledgerEntryRepository = mock(JpaLedgerEntryRepository.class);

	private final IdempotencyService idempotencyService = mock(IdempotencyService.class);

	private final OutboxRepository outboxRepository = mock(OutboxRepository.class);

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private final DefaultWalletService service = new DefaultWalletService(walletRepository, transactionRepository,
			reservationRepository, ledgerEntryRepository, idempotencyService, outboxRepository, objectMapper,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void creditsWalletWithLedgerEntryOutboxEventAndCompletedIdempotency() {
		IdempotencyRecord idempotency = idempotency(IdempotentOperationType.DEPOSIT_CALLBACK, "deposit-key", "provider");
		when(idempotencyService.startOrGet(any(), any(), any(), any())).thenReturn(idempotency);
		when(walletRepository.findByPrincipalIdAndCurrencyCode("user-1", "KES")).thenReturn(Optional.empty());
		when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0),
				UUID.fromString("00000000-0000-0000-0000-000000000001")));
		when(transactionRepository.save(any(WalletTransactionEntity.class))).thenAnswer(invocation -> withId(
				invocation.getArgument(0), UUID.fromString("00000000-0000-0000-0000-000000000002")));

		WalletOperationResult result = service.credit(new CreditWalletCommand("user-1", Money.ofMinor(1_500, KES),
				new IdempotencyKey("deposit-key"), IdempotentOperationType.DEPOSIT_CALLBACK, TransactionType.DEPOSIT,
				"PAYMENT", "payment-1", "provider-1", "provider"));

		assertThat(result.balanceMinor()).isEqualTo(1_500);
		assertThat(result.reservedMinor()).isZero();
		verify(ledgerEntryRepository).save(any(LedgerEntryEntity.class));
		verify(outboxRepository).save(any(OutboxEvent.class));
		verify(idempotencyService).complete(idempotency, 200, objectMapper.valueToTree(result).toString());
	}

	@Test
	void reservesAvailableFundsWithoutWritingBalanceLedgerEntry() {
		IdempotencyRecord idempotency = idempotency(IdempotentOperationType.BET_PLACEMENT, "bet-key", "user-1");
		WalletEntity wallet = wallet(UUID.fromString("00000000-0000-0000-0000-000000000011"), 5_000);
		when(idempotencyService.startOrGet(any(), any(), any(), any())).thenReturn(idempotency);
		when(walletRepository.findByPrincipalIdAndCurrencyCode("user-1", "KES")).thenReturn(Optional.of(wallet));
		when(transactionRepository.save(any(WalletTransactionEntity.class))).thenAnswer(invocation -> withId(
				invocation.getArgument(0), UUID.fromString("00000000-0000-0000-0000-000000000012")));
		when(reservationRepository.save(any(FundReservationEntity.class))).thenAnswer(invocation -> withId(
				invocation.getArgument(0), UUID.fromString("00000000-0000-0000-0000-000000000013")));

		WalletOperationResult result = service.reserve(new ReserveFundsCommand("user-1", Money.ofMinor(2_000, KES),
				new IdempotencyKey("bet-key"), "BET", "bet-1", NOW.plusSeconds(300), "user-1"));

		assertThat(result.balanceMinor()).isEqualTo(5_000);
		assertThat(result.reservedMinor()).isEqualTo(2_000);
		assertThat(result.availableMinor()).isEqualTo(3_000);
		verify(ledgerEntryRepository, never()).save(any());
		verify(outboxRepository).save(any(OutboxEvent.class));
	}

	@Test
	void capturesReservationWithLedgerDebit() {
		IdempotencyRecord idempotency = idempotency(IdempotentOperationType.SETTLEMENT_REPLAY, "capture-key", "system");
		UUID walletId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000022");
		UUID reservationId = UUID.fromString("00000000-0000-0000-0000-000000000023");
		WalletEntity wallet = wallet(walletId, 5_000);
		wallet.reserve(2_000, NOW);
		WalletTransactionEntity transaction = transaction(walletId, transactionId, 2_000);
		FundReservationEntity reservation = reservation(walletId, transactionId, reservationId, 2_000);
		when(idempotencyService.startOrGet(any(), any(), any(), any())).thenReturn(idempotency);
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
		when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
		when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

		WalletOperationResult result = service.captureReservation(new CaptureReservationCommand(reservationId,
				new IdempotencyKey("capture-key"), IdempotentOperationType.SETTLEMENT_REPLAY, "system"));

		assertThat(result.balanceMinor()).isEqualTo(3_000);
		assertThat(result.reservedMinor()).isZero();
		verify(ledgerEntryRepository).save(any(LedgerEntryEntity.class));
		verify(outboxRepository).save(any(OutboxEvent.class));
	}

	@Test
	void returnsStoredResultForCompletedIdempotencyReplay() throws Exception {
		WalletOperationResult stored = new WalletOperationResult(UUID.fromString("00000000-0000-0000-0000-000000000031"),
				null, null, "KES", 1_000, 0, 1_000);
		IdempotencyRecord idempotency = idempotency(IdempotentOperationType.DEPOSIT_CALLBACK, "deposit-key", "provider");
		idempotency.complete(200, objectMapper.writeValueAsString(stored), NOW);
		when(idempotencyService.startOrGet(any(), any(), any(), any())).thenReturn(idempotency);

		WalletOperationResult result = service.credit(new CreditWalletCommand("user-1", Money.ofMinor(1_000, KES),
				new IdempotencyKey("deposit-key"), IdempotentOperationType.DEPOSIT_CALLBACK, TransactionType.DEPOSIT,
				"PAYMENT", "payment-1", "provider-1", "provider"));

		assertThat(result).isEqualTo(stored);
		verify(walletRepository, never()).save(any());
		verify(outboxRepository, never()).save(any());
	}

	private IdempotencyRecord idempotency(IdempotentOperationType operationType, String key, String actorId) {
		return IdempotencyRecord.start(operationType, new IdempotencyKey(key), actorId, "hash", NOW.plusSeconds(600), NOW);
	}

	private WalletEntity wallet(UUID id, long balanceMinor) {
		WalletEntity wallet = WalletEntity.create("user-1", "KES", NOW);
		withId(wallet, id);
		if (balanceMinor > 0) {
			wallet.credit(balanceMinor, NOW);
		}
		return wallet;
	}

	private WalletTransactionEntity transaction(UUID walletId, UUID transactionId, long amountMinor) {
		WalletTransactionEntity transaction = WalletTransactionEntity.pending(walletId, TransactionType.BET_STAKE,
				amountMinor, "KES", "bet-key", "BET", "bet-1", null, "{}", NOW);
		return withId(transaction, transactionId);
	}

	private FundReservationEntity reservation(UUID walletId, UUID transactionId, UUID reservationId, long amountMinor) {
		FundReservationEntity reservation = FundReservationEntity.held(walletId, transactionId, amountMinor, "KES",
				"bet-key", "BET", "bet-1", NOW.plusSeconds(300), NOW);
		return withId(reservation, reservationId);
	}

	private <T> T withId(T entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
