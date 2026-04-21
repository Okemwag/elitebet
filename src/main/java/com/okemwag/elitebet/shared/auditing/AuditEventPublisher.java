package com.okemwag.elitebet.shared.auditing;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Service
@ConditionalOnBean(AuditEventRepository.class)
public class AuditEventPublisher {

	private final AuditEventRepository repository;

	private final ObjectMapper objectMapper;

	private final Clock clock;

	public AuditEventPublisher(AuditEventRepository repository, ObjectMapper objectMapper, Clock clock) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public void publish(AuditContext context, AuditAction action, AuditCategory category, AuditMetadata metadata) {
		repository.save(new AuditEvent(context, action, category, metadata, toJson(metadata.values()), clock.instant()));
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Unable to serialize audit metadata", exception);
		}
	}
}
