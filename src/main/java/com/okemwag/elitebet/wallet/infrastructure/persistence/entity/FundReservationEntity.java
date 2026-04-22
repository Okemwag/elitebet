package com.okemwag.elitebet.wallet.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.okemwag.elitebet.wallet.domain.enums.ReservationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "fund_reservations", schema = "elitebet")
public class FundReservationEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ReservationStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 160)
	private String idempotencyKey;

	@Column(name = "reference_type", nullable = false, length = 80)
	private String referenceType;

	@Column(name = "reference_id", nullable = false, length = 160)
	private String referenceId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "captured_at")
	private Instant capturedAt;

	@Column(name = "released_at")
	private Instant releasedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected FundReservationEntity() {
	}

	private FundReservationEntity(UUID walletId, UUID transactionId, long amountMinor, String currencyCode,
			String idempotencyKey, String referenceType, String referenceId, Instant expiresAt, Instant now) {
		this.walletId = walletId;
		this.transactionId = transactionId;
		this.amountMinor = amountMinor;
		this.currencyCode = currencyCode;
		this.status = ReservationStatus.HELD;
		this.idempotencyKey = idempotencyKey;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.expiresAt = expiresAt;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static FundReservationEntity held(UUID walletId, UUID transactionId, long amountMinor, String currencyCode,
			String idempotencyKey, String referenceType, String referenceId, Instant expiresAt, Instant now) {
		return new FundReservationEntity(walletId, transactionId, amountMinor, currencyCode, idempotencyKey,
				referenceType, referenceId, expiresAt, now);
	}

	public void capture(Instant now) {
		requireHeld();
		this.status = ReservationStatus.CAPTURED;
		this.capturedAt = now;
		this.updatedAt = now;
	}

	public void release(Instant now) {
		requireHeld();
		this.status = ReservationStatus.RELEASED;
		this.releasedAt = now;
		this.updatedAt = now;
	}

	private void requireHeld() {
		if (status != ReservationStatus.HELD) {
			throw new IllegalStateException("reservation is not held");
		}
	}

	public UUID id() {
		return id;
	}

	public UUID walletId() {
		return walletId;
	}

	public UUID transactionId() {
		return transactionId;
	}

	public long amountMinor() {
		return amountMinor;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public ReservationStatus status() {
		return status;
	}
}
