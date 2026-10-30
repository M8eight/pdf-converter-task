package com.task.converter.application.service;

import com.task.converter.application.dto.FileConvertedEvent;
import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.enums.ConversionStatus;
import com.task.converter.application.exception.ErrorCode;
import com.task.converter.infrastructure.persistence.entity.Outbox;
import com.task.converter.infrastructure.persistence.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.s3.bucket-name}")
    String exportBucketName;

    @Value("${app.timeouts.outbox.lease-seconds}")
    int leaseSeconds;

    @Value("${app.timeouts.outbox.max-attempts}")
    int maxAttempts;

    @Transactional
    public List<Outbox> claimBatch(UUID lockedToken, int batchSize) {
        List<UUID> ids = outboxRepository.findPendingForUpdate(batchSize);

        if (ids.isEmpty()) {
            return List.of();
        }

        outboxRepository.setProcessing(ids, lockedToken, leaseSeconds);

        return outboxRepository.findAllById(ids);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setSuccess(UUID id, UUID lockedToken) {
        outboxRepository.setSuccess(id, lockedToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryOrFail(Outbox outbox, String error, UUID lockedToken) {
        if (outbox.getRetryAttempts() >= maxAttempts) {
            outboxRepository.setFailed(outbox.getId(), lockedToken, error);
            return;
        }

        Instant nextAttemptAt = Instant.now().plusSeconds(
                Math.min(
                        (long) maxAttempts * leaseSeconds,
                        (long) Math.pow(2, outbox.getRetryAttempts()
                    )
                )
        );

        outboxRepository.setPending(
                outbox.getId(),
                nextAttemptAt,
                error,
                lockedToken
        );

    }

    @Transactional
    public void createOutbox(RequestedFileEvent event, String fileId, ConversionStatus status, ErrorCode errorCode, String errorMessage) {
        UUID newEventId = UUID.randomUUID();

        FileConvertedEvent payload = FileConvertedEvent.builder()
                .eventId(newEventId)
                .correlationId(event.eventId())
                .bucket(exportBucketName)
                .objectKey(fileId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();

        int inserted = outboxRepository.upsert(
                newEventId,
                fileId,
                event.eventId(),
                objectMapper.valueToTree(payload),
                status.name()
        );

        if (inserted == 0) {
            log.info(
                    "Outbox для correlationId={} уже существует, пропускаем",
                    event.eventId()
            );
        }
    }

}
