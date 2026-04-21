package com.okemwag.elitebet.shared.idempotency;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_records", schema = "elitebet",
		uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_operation_key_actor",
				columnNames = { "operation_type", "idempotency_key", "actor_id" }))
public class IdempotencyRecord {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false, length = 80)
	private IdempotentOperationType operationType;

	@Column(name = "idempotency_key", nullable = false, length = 160)
	private String idempotencyKey;

	@Column(name = "actor_id", nullable = false, length = 160)
	private String actorId;

	@Column(name = "request_hash", nullable = false, length = 128)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private IdempotencyStatus status;

	@Column(name = "response_status")
	private Integer responseStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "response_body", columnDefinition = "jsonb")
	private String responseBody;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected IdempotencyRecord() {
	}

	private IdempotencyRecord(IdempotentOperationType operationType, IdempotencyKey idempotencyKey, String actorId,
			String requestHash, Instant lockedUntil, Instant now) {
		this.operationType = operationType;
		this.idempotencyKey = idempotencyKey.value();
		this.actorId = actorId;
		this.requestHash = requestHash;
		this.lockedUntil = lockedUntil;
		this.status = IdempotencyStatus.IN_PROGRESS;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static IdempotencyRecord start(IdempotentOperationType operationType, IdempotencyKey idempotencyKey,
			String actorId, String requestHash, Instant lockedUntil, Instant now) {
		return new IdempotencyRecord(operationType, idempotencyKey, actorId, requestHash, lockedUntil, now);
	}

	public void complete(int responseStatus, String responseBody, Instant now) {
		this.responseStatus = responseStatus;
		this.responseBody = responseBody;
		this.completedAt = now;
		this.status = IdempotencyStatus.COMPLETED;
		this.updatedAt = now;
	}

	public void fail(String failureReason, Instant now) {
		this.failureReason = failureReason;
		this.status = IdempotencyStatus.FAILED;
		this.updatedAt = now;
	}

	public void restart(Instant lockedUntil, Instant now) {
		this.lockedUntil = lockedUntil;
		this.status = IdempotencyStatus.IN_PROGRESS;
		this.failureReason = null;
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public IdempotentOperationType operationType() {
		return operationType;
	}

	public String idempotencyKey() {
		return idempotencyKey;
	}

	public String actorId() {
		return actorId;
	}

	public String requestHash() {
		return requestHash;
	}

	public Instant lockedUntil() {
		return lockedUntil;
	}

	public IdempotencyStatus status() {
		return status;
	}

	public Integer responseStatus() {
		return responseStatus;
	}

	public String responseBody() {
		return responseBody;
	}
}
