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
				clock.instant(), PageRequest.of(0, 100));
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
			event.markFailed(exception.getMessage(), clock.instant().plus(Duration.ofMinutes(1)));
		}
	}
}
