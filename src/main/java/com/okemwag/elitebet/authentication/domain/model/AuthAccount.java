package com.okemwag.elitebet.authentication.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.okemwag.elitebet.authentication.domain.enums.AccountStatus;
import com.okemwag.elitebet.authentication.domain.enums.AuthProvider;
import com.okemwag.elitebet.authentication.domain.enums.MfaStatus;

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
@Table(name = "auth_accounts", schema = "elitebet",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_auth_accounts_principal_id", columnNames = "principal_id"),
				@UniqueConstraint(name = "uq_auth_accounts_email", columnNames = "email"),
				@UniqueConstraint(name = "uq_auth_accounts_provider_user", columnNames = { "provider",
						"provider_user_id" }) })
public class AuthAccount {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "principal_id", nullable = false, length = 160)
	private String principalId;

	@Column(nullable = false, length = 160)
	private String username;

	@Column(nullable = false, length = 320)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AccountStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "mfa_status", nullable = false, length = 40)
	private MfaStatus mfaStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 160)
	private String providerUserId;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected AuthAccount() {
	}

	private AuthAccount(String principalId, String username, String email, AuthProvider provider, String providerUserId,
			boolean emailVerified, Instant now) {
		this.principalId = principalId;
		this.username = username;
		this.email = email.toLowerCase();
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.emailVerified = emailVerified;
		this.status = emailVerified ? AccountStatus.ACTIVE : AccountStatus.PENDING_ACTIVATION;
		this.mfaStatus = MfaStatus.NOT_CONFIGURED;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static AuthAccount create(String principalId, String username, String email, AuthProvider provider,
			String providerUserId, boolean emailVerified, Instant now) {
		return new AuthAccount(principalId, username, email, provider, providerUserId, emailVerified, now);
	}

	public void recordLogin(Instant now) {
		this.lastLoginAt = now;
		this.failedLoginAttempts = 0;
		this.updatedAt = now;
	}

	public void lock(Instant lockedUntil, Instant now) {
		this.status = AccountStatus.LOCKED;
		this.lockedUntil = lockedUntil;
		this.updatedAt = now;
	}

	public void unlock(Instant now) {
		this.status = AccountStatus.ACTIVE;
		this.lockedUntil = null;
		this.failedLoginAttempts = 0;
		this.updatedAt = now;
	}

	public void disable(Instant now) {
		this.status = AccountStatus.DISABLED;
		this.lockedUntil = null;
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public String principalId() {
		return principalId;
	}

	public String username() {
		return username;
	}

	public String email() {
		return email;
	}

	public AccountStatus status() {
		return status;
	}

	public MfaStatus mfaStatus() {
		return mfaStatus;
	}

	public AuthProvider provider() {
		return provider;
	}

	public String providerUserId() {
		return providerUserId;
	}

	public boolean emailVerified() {
		return emailVerified;
	}

	public Instant lockedUntil() {
		return lockedUntil;
	}

	public Instant lastLoginAt() {
		return lastLoginAt;
	}
}
