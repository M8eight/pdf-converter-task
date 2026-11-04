package com.task.converter.infrastructure.persistence.entity;

import com.task.converter.application.enums.ConversionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "inbox")
public class Inbox {

    @Id
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    private ConversionStatus status;

    private Instant lockedUntil;

    private UUID lockedToken;

    private int retryAttempts;

    private Instant processedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
