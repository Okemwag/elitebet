package com.okemwag.elitebet.wallet.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.wallet.domain.enums.TransactionType;
import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.WalletTransactionEntity;

public interface JpaWalletTransactionRepository extends JpaRepository<WalletTransactionEntity, UUID> {

	Optional<WalletTransactionEntity> findByWalletIdAndTransactionTypeAndIdempotencyKey(UUID walletId,
			TransactionType transactionType, String idempotencyKey);
}
