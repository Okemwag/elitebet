package com.okemwag.elitebet.wallet.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.okemwag.elitebet.wallet.domain.enums.TransactionStatus;
import com.okemwag.elitebet.wallet.domain.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "wallet_transactions", schema = "elitebet")
public class WalletTransactionEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 40)
	private TransactionType transactionType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TransactionStatus status;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "idempotency_key", length = 160)
	private String idempotencyKey;

	@Column(name = "reference_type", length = 80)
	private String referenceType;

	@Column(name = "reference_id", length = 160)
	private String referenceId;

	@Column(name = "external_reference", length = 160)
	private String externalReference;

	@Column(name = "failure_reason")
	private String failureReason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	@Column(name = "posted_at")
	private Instant postedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected WalletTransactionEntity() {
	}

	private WalletTransactionEntity(UUID walletId, TransactionType transactionType, long amountMinor,
			String currencyCode, String idempotencyKey, String referenceType, String referenceId,
			String externalReference, String metadata, Instant now) {
		this.walletId = walletId;
		this.transactionType = transactionType;
		this.status = TransactionStatus.PENDING;
		this.amountMinor = amountMinor;
		this.currencyCode = currencyCode;
		this.idempotencyKey = idempotencyKey;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.externalReference = externalReference;
		this.metadata = metadata;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static WalletTransactionEntity pending(UUID walletId, TransactionType transactionType, long amountMinor,
			String currencyCode, String idempotencyKey, String referenceType, String referenceId,
			String externalReference, String metadata, Instant now) {
		return new WalletTransactionEntity(walletId, transactionType, amountMinor, currencyCode, idempotencyKey,
				referenceType, referenceId, externalReference, metadata, now);
	}

	public void post(Instant now) {
		this.status = TransactionStatus.POSTED;
		this.postedAt = now;
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public UUID walletId() {
		return walletId;
	}

	public TransactionType transactionType() {
		return transactionType;
	}

	public long amountMinor() {
		return amountMinor;
	}

	public String currencyCode() {
		return currencyCode;
	}
}
