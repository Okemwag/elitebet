package com.okemwag.elitebet.wallet.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.WalletEntity;

public interface JpaWalletRepository extends JpaRepository<WalletEntity, UUID> {

	Optional<WalletEntity> findByPrincipalIdAndCurrencyCode(String principalId, String currencyCode);
}
