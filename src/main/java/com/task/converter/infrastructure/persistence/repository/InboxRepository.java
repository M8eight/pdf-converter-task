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
        insert into inbox (event_id, status, locked_until, locked_token, retry_attempts)
        values (:id, 'PROCESSING', now() + :leaseSec * interval '1 second', :lockedToken, 0)
        on conflict (event_id) do update
            set status = 'PROCESSING',
                locked_until = now() + :leaseSec * interval '1 second',
                retry_attempts = inbox.retry_attempts + 1,
                locked_token = :lockedToken
            where inbox.status = 'PENDING'
                or (inbox.status = 'PROCESSING' and inbox.locked_until < now())
    """, nativeQuery = true)
    int claim(@Param("id") UUID id, @Param("leaseSec") long leaseSec, @Param("lockedToken") UUID lockedToken);

    @Modifying
    @Query(value = """
        update inbox
        set status = 'SUCCESS',
            locked_until = null,
            processed_at = now()
        where event_id = :id
            and status = 'PROCESSING'
            and locked_token = :lockedToken
    """, nativeQuery = true)
    int setSuccessIfProcessing(@Param("id") UUID id, @Param("lockedToken") UUID lockedToken);

    @Modifying
    @Query(value = """
        update inbox
        set status = 'FAILED',
            locked_until = null
        where event_id = :id
            and status in ('PROCESSING', 'PENDING')
    """, nativeQuery = true)
    int setFailedIfNotTerminal(@Param("id") UUID id);

    @Modifying
    @Query(value = """
        update inbox
        set status = 'PENDING',
            locked_until = null
        where event_id = :id
            and status = 'PROCESSING'
            and locked_token = :lockedToken
    """, nativeQuery = true)
    int setPendingIfProcessing(@Param("id") UUID id, @Param("lockedToken") UUID lockedToken);

}
