package com.okemwag.elitebet.shared.auditing;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "audit_events", schema = "elitebet")
public class AuditEvent {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "actor_id", nullable = false, length = 160)
	private String actorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 40)
	private AuditActorType actorType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 80)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 80)
	private AuditCategory category;

	@Column(name = "entity_type", nullable = false, length = 120)
	private String entityType;

	@Column(name = "entity_id", length = 160)
	private String entityId;

	@Column(name = "correlation_id", length = 160)
	private String correlationId;

	@Column(name = "ip_address", length = 80)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_state", columnDefinition = "jsonb")
	private String beforeState;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_state", columnDefinition = "jsonb")
	private String afterState;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected AuditEvent() {
	}

	public AuditEvent(AuditContext context, AuditAction action, AuditCategory category, AuditMetadata metadata,
			String metadataJson, Instant occurredAt) {
		this.actorId = context.actorId();
		this.actorType = context.actorType();
		this.correlationId = context.correlationId();
		this.ipAddress = context.ipAddress();
		this.userAgent = context.userAgent();
		this.action = action;
		this.category = category;
		this.entityType = metadata.entityType();
		this.entityId = metadata.entityId();
		this.beforeState = metadata.beforeState();
		this.afterState = metadata.afterState();
		this.metadata = metadataJson;
		this.occurredAt = occurredAt;
	}

	public UUID id() {
		return id;
	}
}
