package com.task.converter.infrastructure.persistence.repository;

import com.task.converter.infrastructure.persistence.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, UUID> {

    @Modifying
    @Query(value = """
        insert into inbox (event_id, processed_at)
            values (:eventId, now())
            on conflict (event_id) do nothing
    """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId);

}
