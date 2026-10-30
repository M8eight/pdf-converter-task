package com.task.converter.infrastructure.persistence.entity;

import com.task.converter.application.enums.ConversionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "outbox")
@EntityListeners(AuditingEntityListener.class)
public class Outbox {

    @Id
    private UUID id;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    private ConversionStatus status;

    private Instant lockedUntil;

    private UUID lockedToken;

    @Column(name = "retry_attempts", nullable = false)
    private int retryAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

}
