package com.okemwag.elitebet.shared.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class OutboxProcessorTest {

	private static final Instant NOW = Instant.parse("2026-04-22T00:00:00Z");

	private final OutboxRepository repository = mock(OutboxRepository.class);

	private final OutboxPublisher publisher = mock(OutboxPublisher.class);

	private final OutboxProcessor processor = new OutboxProcessor(repository, List.of(publisher),
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void publishesReadyEventsAndMarksThemPublished() {
		OutboxEvent event = OutboxEvent.create(OutboxEventType.INTEGRATION_EVENT, "Bet", "bet-1", "bet.placed",
				"{\"betId\":\"bet-1\"}", "{}", NOW, NOW);
		when(repository.findReadyEvents(eq(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED)), eq(NOW),
				any(Pageable.class))).thenReturn(List.of(event));

		int processed = processor.processReadyEvents();

		assertThat(processed).isEqualTo(1);
		assertThat(event.status()).isEqualTo(OutboxStatus.PUBLISHED);
		verify(publisher).publish(event);
	}

	@Test
	void marksEventFailedAndIncrementsRetryCountWhenPublisherFails() {
		OutboxEvent event = OutboxEvent.create(OutboxEventType.INTEGRATION_EVENT, "Bet", "bet-1", "bet.placed",
				"{\"betId\":\"bet-1\"}", "{}", NOW, NOW);
		when(repository.findReadyEvents(eq(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED)), eq(NOW),
				any(Pageable.class))).thenReturn(List.of(event));
		org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(event);

		int processed = processor.processReadyEvents();

		assertThat(processed).isEqualTo(1);
		assertThat(event.status()).isEqualTo(OutboxStatus.FAILED);
		assertThat(event.retryCount()).isEqualTo(1);
	}
}
