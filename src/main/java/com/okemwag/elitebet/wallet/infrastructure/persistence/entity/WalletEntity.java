package com.okemwag.elitebet.wallet.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.okemwag.elitebet.wallet.domain.enums.WalletStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "wallets", schema = "elitebet",
		uniqueConstraints = @UniqueConstraint(name = "uq_wallets_principal_currency",
				columnNames = { "principal_id", "currency_code" }))
public class WalletEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "principal_id", nullable = false, length = 160)
	private String principalId;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private WalletStatus status;

	@Column(name = "balance_minor", nullable = false)
	private long balanceMinor;

	@Column(name = "reserved_minor", nullable = false)
	private long reservedMinor;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected WalletEntity() {
	}

	private WalletEntity(String principalId, String currencyCode, Instant now) {
		this.principalId = principalId;
		this.currencyCode = currencyCode;
		this.status = WalletStatus.ACTIVE;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static WalletEntity create(String principalId, String currencyCode, Instant now) {
		return new WalletEntity(principalId, currencyCode, now);
	}

	public void credit(long amountMinor, Instant now) {
		requireActive();
		requirePositive(amountMinor);
		this.balanceMinor = Math.addExact(this.balanceMinor, amountMinor);
		this.updatedAt = now;
	}

	public void reserve(long amountMinor, Instant now) {
		requireActive();
		requirePositive(amountMinor);
		if (availableMinor() < amountMinor) {
			throw new IllegalStateException("insufficient available wallet balance");
		}
		this.reservedMinor = Math.addExact(this.reservedMinor, amountMinor);
		this.updatedAt = now;
	}

	public void captureReserved(long amountMinor, Instant now) {
		requireActive();
		requirePositive(amountMinor);
		if (reservedMinor < amountMinor) {
			throw new IllegalStateException("insufficient reserved wallet balance");
		}
		this.reservedMinor = Math.subtractExact(this.reservedMinor, amountMinor);
		this.balanceMinor = Math.subtractExact(this.balanceMinor, amountMinor);
		this.updatedAt = now;
	}

	public void releaseReserved(long amountMinor, Instant now) {
		requireActive();
		requirePositive(amountMinor);
		if (reservedMinor < amountMinor) {
			throw new IllegalStateException("insufficient reserved wallet balance");
		}
		this.reservedMinor = Math.subtractExact(this.reservedMinor, amountMinor);
		this.updatedAt = now;
	}

	public long availableMinor() {
		return balanceMinor - reservedMinor;
	}

	private void requireActive() {
		if (status != WalletStatus.ACTIVE) {
			throw new IllegalStateException("wallet is not active");
		}
	}

	private void requirePositive(long amountMinor) {
		if (amountMinor <= 0) {
			throw new IllegalArgumentException("amount must be greater than zero");
		}
	}

	public UUID id() {
		return id;
	}

	public String principalId() {
		return principalId;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public WalletStatus status() {
		return status;
	}

	public long balanceMinor() {
		return balanceMinor;
	}

	public long reservedMinor() {
		return reservedMinor;
	}
}
