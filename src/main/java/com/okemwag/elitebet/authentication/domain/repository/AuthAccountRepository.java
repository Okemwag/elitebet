package com.okemwag.elitebet.authentication.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.authentication.domain.model.AuthAccount;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

	boolean existsByEmail(String email);

	Optional<AuthAccount> findByPrincipalId(String principalId);

	Optional<AuthAccount> findByProviderUserId(String providerUserId);
}
