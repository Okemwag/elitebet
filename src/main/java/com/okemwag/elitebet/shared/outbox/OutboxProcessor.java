package com.okemwag.elitebet.shared.outbox;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(OutboxRepository.class)
public class OutboxProcessor {

	private static final int BATCH_SIZE = 100;

	private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(1);

	private static final Duration MAX_RETRY_DELAY = Duration.ofHours(1);

	private final OutboxRepository repository;

	private final List<OutboxPublisher> publishers;

	private final Clock clock;

	public OutboxProcessor(OutboxRepository repository, List<OutboxPublisher> publishers, Clock clock) {
		this.repository = repository;
		this.publishers = publishers;
		this.clock = clock;
	}

	@Transactional
	public int processReadyEvents() {
		List<OutboxEvent> events = repository.findReadyEvents(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
				clock.instant(), PageRequest.of(0, BATCH_SIZE));
		events.forEach(this::publish);
		return events.size();
	}

	private void publish(OutboxEvent event) {
		try {
			event.markProcessing();
			for (OutboxPublisher publisher : publishers) {
				publisher.publish(event);
			}
			event.markPublished(clock.instant());
		}
		catch (RuntimeException exception) {
			event.markFailed(exception.getMessage(), clock.instant().plus(nextRetryDelay(event)));
		}
	}

	private Duration nextRetryDelay(OutboxEvent event) {
		long multiplier = 1L << Math.min(event.retryCount(), 6);
		Duration delay = BASE_RETRY_DELAY.multipliedBy(multiplier);
		return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
	}
}
