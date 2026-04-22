package com.okemwag.elitebet.wallet.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.okemwag.elitebet.wallet.domain.enums.LedgerEntryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries", schema = "elitebet")
public class LedgerEntryEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "reservation_id")
	private UUID reservationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 40)
	private LedgerEntryType entryType;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "balance_after_minor", nullable = false)
	private long balanceAfterMinor;

	@Column(name = "reference_type", length = 80)
	private String referenceType;

	@Column(name = "reference_id", length = 160)
	private String referenceId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected LedgerEntryEntity() {
	}

	private LedgerEntryEntity(UUID walletId, UUID transactionId, UUID reservationId, LedgerEntryType entryType,
			long amountMinor, String currencyCode, long balanceAfterMinor, String referenceType, String referenceId,
			String metadata, Instant now) {
		this.walletId = walletId;
		this.transactionId = transactionId;
		this.reservationId = reservationId;
		this.entryType = entryType;
		this.amountMinor = amountMinor;
		this.currencyCode = currencyCode;
		this.balanceAfterMinor = balanceAfterMinor;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.metadata = metadata;
		this.createdAt = now;
	}

	public static LedgerEntryEntity create(UUID walletId, UUID transactionId, UUID reservationId,
			LedgerEntryType entryType, long amountMinor, String currencyCode, long balanceAfterMinor,
			String referenceType, String referenceId, String metadata, Instant now) {
		return new LedgerEntryEntity(walletId, transactionId, reservationId, entryType, amountMinor, currencyCode,
				balanceAfterMinor, referenceType, referenceId, metadata, now);
	}
}
