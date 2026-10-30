package com.task.converter.infrastructure.persistence.repository;

import com.task.converter.infrastructure.persistence.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    @Modifying
    @Query(value = """
        insert into outbox (id, file_id, correlation_id, payload, status)
        values (:id, :fileId, :correlationId, :payload, :status)
        on conflict (correlation_id) do nothing
    """, nativeQuery = true)
    int upsert(@Param("id") UUID id,
               @Param("fileId") String fileId,
               @Param("correlationId") UUID correlationId,
               @Param("payload") JsonNode payload,
               @Param("status") String status);

    @Query(value = """
        select id from outbox
            where (
                status = 'PENDING' and next_attempt_at <= now()
            )
            or (
                status = 'PROCESSING' and (locked_until is null or locked_until < now()) 
            )
            order by next_attempt_at
            limit :limit
            for update skip locked
    """, nativeQuery = true)
    List<UUID> findPendingForUpdate(@Param("limit") int limit);

    @Modifying
    @Query(value = """
        update outbox
        set status = 'PROCESSING',
            locked_until = now() + :leaseSec * interval '1 second',
            locked_token = :lockedToken
        where id in (:ids)
    """, nativeQuery = true)
    void setProcessing(@Param("ids") List<UUID> ids, @Param("lockedToken") UUID lockedToken, @Param("leaseSec") long leaseSec);

    @Modifying
    @Query(value = """
        update outbox
        set status = 'SUCCESS',
            published_at = now(),
            locked_token = null,
            locked_until = null,
            last_error = null,
            next_attempt_at = null
        where id = :id
            and status = 'PROCESSING'
            and locked_token = :lockedToken
    """, nativeQuery = true)
    int setSuccess(@Param("id") UUID id, @Param("lockedToken") UUID lockedToken);

    @Modifying
    @Query(value = """
        update outbox
        set status = 'PENDING',
            next_attempt_at = :nextAttemptAt,
            locked_until = null,
            locked_token = null,
            last_error = :errorMessage,
            retry_attempts = retry_attempts + 1
        where id = :id
            and status = 'PROCESSING'
            and locked_token = :lockedToken
    """, nativeQuery = true)
    int setPending(@Param("id") UUID id,
                    @Param("nextAttemptAt") Instant nextAttemptAt,
                    @Param("errorMessage") String errorMessage,
                    @Param("lockedToken") UUID lockedToken);

    @Modifying
    @Query(value = """
        update outbox
        set status = 'FAILED',
            next_attempt_at = null,
            locked_until = null,
            locked_token = null,
            last_error = :errorMessage,
            next_attempt_at = null
        where id = :id
            and status = 'PROCESSING'
            and locked_token = :lockedToken
    """, nativeQuery = true)
    int setFailed(@Param("id") UUID id,
                  @Param("lockedToken") UUID lockedToken,
                  @Param("errorMessage") String errorMessage);

//    boolean existsByCorrelationId(UUID correlationId);

//    @Modifying
//    @Query(value = """
//        insert into outbox (id, file_id, correlation_id, payload, status, last_error, next_attempt_at)
//        values (:id, :fileId, :correlationId, :payload, 'PENDING', :lastError, now())
//    """, nativeQuery = true)
//    int insertFailureEvent(@Param("id") UUID id,
//                           @Param("fileId") String fileId,
//                           @Param("correlationId") UUID correlationId,
//                           @Param("payload") JsonNode payload,
//                           @Param("lastError") String lastError);

}
