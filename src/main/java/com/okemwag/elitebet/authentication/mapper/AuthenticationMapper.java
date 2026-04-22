package com.okemwag.elitebet.authentication.mapper;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.okemwag.elitebet.authentication.application.dto.AuthAccountView;
import com.okemwag.elitebet.authentication.domain.model.AuthAccount;

@Component
public class AuthenticationMapper {

	public AuthAccountView toView(AuthAccount account, Set<String> roles) {
		return new AuthAccountView(account.principalId(), account.accountNumber(), account.username(), account.email(),
				account.emailVerified(), account.status(), account.mfaStatus(), Set.copyOf(roles), account.lockedUntil(),
				account.lastLoginAt());
	}
}
