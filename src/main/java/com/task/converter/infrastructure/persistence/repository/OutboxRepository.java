package com.task.converter.infrastructure.persistence.repository;

import com.task.converter.infrastructure.persistence.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    @Query(value = """
        select * from outbox
            where status = 'PENDING'
            order by created_at
            limit :limit
            for update skip locked
    """, nativeQuery = true)
    List<Outbox> findPendingForUpdate(@Param("limit") int limit);

}
