package com.okemwag.elitebet.shared.outbox;

public interface OutboxPublisher {
	void publish(OutboxEvent event);
}
