package com.okemwag.elitebet.shared.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events", schema = "elitebet")
public class OutboxEvent {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 80)
	private OutboxEventType eventType;

	@Column(name = "aggregate_type", nullable = false, length = 120)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false, length = 160)
	private String aggregateId;

	@Column(name = "event_name", nullable = false, length = 160)
	private String eventName;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String payload;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private OutboxStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_retry_at")
	private Instant nextRetryAt;

	@Column(name = "last_error")
	private String lastError;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Version
	private long version;

	protected OutboxEvent() {
	}

	private OutboxEvent(OutboxEventType eventType, String aggregateType, String aggregateId, String eventName,
			String payload, String metadata, Instant occurredAt, Instant now) {
		this.eventType = eventType;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventName = eventName;
		this.payload = payload;
		this.metadata = metadata;
		this.occurredAt = occurredAt;
		this.createdAt = now;
		this.status = OutboxStatus.PENDING;
	}

	public static OutboxEvent create(OutboxEventType eventType, String aggregateType, String aggregateId,
			String eventName, String payload, String metadata, Instant occurredAt, Instant now) {
		return new OutboxEvent(eventType, aggregateType, aggregateId, eventName, payload, metadata, occurredAt, now);
	}

	public void markProcessing() {
		this.status = OutboxStatus.PROCESSING;
	}

	public void markPublished(Instant now) {
		this.status = OutboxStatus.PUBLISHED;
		this.publishedAt = now;
	}

	public void markFailed(String error, Instant nextRetryAt) {
		this.status = OutboxStatus.FAILED;
		this.lastError = error;
		this.nextRetryAt = nextRetryAt;
		this.retryCount++;
	}

	public UUID id() {
		return id;
	}

	public OutboxEventType eventType() {
		return eventType;
	}

	public OutboxStatus status() {
		return status;
	}

	public String payload() {
		return payload;
	}
}
