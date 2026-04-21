package com.okemwag.elitebet.authentication.application;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okemwag.elitebet.authentication.domain.enums.AccountStatus;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;
import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;

@Service
public class AccountStateAccessService {

	private final AuthAccountRepository repository;

	private final Clock clock;

	public AccountStateAccessService(AuthAccountRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AccountAccessDecision accessDecision(String principalId, String path) {
		return repository.findByPrincipalId(principalId)
			.map(account -> decisionFor(account, path))
			.orElse(AccountAccessDecision.rejected(AccountAccessRejection.ACCOUNT_NOT_REGISTERED));
	}

	private AccountAccessDecision decisionFor(AuthAccount account, String path) {
		if (account.status() == AccountStatus.DISABLED) {
			return AccountAccessDecision.rejected(AccountAccessRejection.ACCOUNT_DISABLED);
		}
		if (account.status() == AccountStatus.LOCKED) {
			if (account.lockedUntil() == null || account.lockedUntil().isAfter(clock.instant())) {
				return AccountAccessDecision.rejected(AccountAccessRejection.ACCOUNT_LOCKED);
			}
			return AccountAccessDecision.allowed();
		}
		if (account.status() == AccountStatus.PENDING_ACTIVATION && !isPendingAllowedPath(path)) {
			return AccountAccessDecision.rejected(AccountAccessRejection.ACCOUNT_PENDING_ACTIVATION);
		}
		return AccountAccessDecision.allowed();
	}

	private boolean isPendingAllowedPath(String path) {
		return path.equals("/api/v1/auth/me") || path.equals("/api/v1/auth/session")
				|| path.equals("/api/v1/auth/logout");
	}

	public record AccountAccessDecision(boolean allowed, AccountAccessRejection rejection) {
		public static AccountAccessDecision allowed() {
			return new AccountAccessDecision(true, null);
		}

		public static AccountAccessDecision rejected(AccountAccessRejection rejection) {
			return new AccountAccessDecision(false, rejection);
		}
	}

	public enum AccountAccessRejection {
		ACCOUNT_NOT_REGISTERED("Account is not registered", 403),
		ACCOUNT_PENDING_ACTIVATION("Account activation is required", 403),
		ACCOUNT_LOCKED("Account is locked", 423),
		ACCOUNT_DISABLED("Account is disabled", 403);

		private final String message;

		private final int status;

		AccountAccessRejection(String message, int status) {
			this.message = message;
			this.status = status;
		}

		public String message() {
			return message;
		}

		public int status() {
			return status;
		}
	}
}
