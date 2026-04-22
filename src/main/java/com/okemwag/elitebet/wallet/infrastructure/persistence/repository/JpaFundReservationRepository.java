package com.okemwag.elitebet.wallet.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.FundReservationEntity;

public interface JpaFundReservationRepository extends JpaRepository<FundReservationEntity, UUID> {

	Optional<FundReservationEntity> findByWalletIdAndIdempotencyKey(UUID walletId, String idempotencyKey);

	Optional<FundReservationEntity> findByWalletIdAndReferenceTypeAndReferenceId(UUID walletId, String referenceType,
			String referenceId);
}
