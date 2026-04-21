package com.okemwag.elitebet.shared.auditing;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
