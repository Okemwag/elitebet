package com.okemwag.elitebet.shared.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
	@Query("""
			select event
			from OutboxEvent event
			where event.status in :statuses
			  and (event.nextRetryAt is null or event.nextRetryAt <= :now)
			order by event.createdAt asc
			""")
	List<OutboxEvent> findReadyEvents(@Param("statuses") Collection<OutboxStatus> statuses, @Param("now") Instant now,
			Pageable pageable);
}
