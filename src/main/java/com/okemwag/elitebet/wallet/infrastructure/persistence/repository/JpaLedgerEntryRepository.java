package com.okemwag.elitebet.wallet.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.wallet.infrastructure.persistence.entity.LedgerEntryEntity;

public interface JpaLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
}
