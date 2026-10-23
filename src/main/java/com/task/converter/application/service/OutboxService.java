package com.task.converter.application.service;

import com.task.converter.application.dto.FileConvertedEvent;
import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.enums.ConversionStatus;
import com.task.converter.application.exception.ErrorCode;
import com.task.converter.infrastructure.persistence.entity.Outbox;
import com.task.converter.infrastructure.persistence.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Outbox> findPendingEvents(int limit) {
        return outboxRepository.findPendingForUpdate(limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setSuccess(UUID id) {
        outboxRepository.findById(id).ifPresent(outbox -> {
            outbox.setStatus(ConversionStatus.SUCCESS);
            outbox.setPublishedAt(Instant.now());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setFailed(UUID id) {
        outboxRepository.findById(id).ifPresent(outbox -> {
            outbox.setStatus(ConversionStatus.FAILED);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutbox(RequestedFileEvent event, String fileId,
                            ConversionStatus status, ErrorCode errorCode, String errorMessage) {
        UUID newEventId = UUID.randomUUID();

        FileConvertedEvent payload = FileConvertedEvent.builder()
                .eventId(newEventId)
                .correlationId(event.eventId())
                .bucket(event.bucket())
                .objectKey(fileId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();

        Outbox outbox = Outbox.builder()
                .id(newEventId)
                .fileId(fileId)
                .payload(objectMapper.valueToTree(payload))
                .status(status)
                .build();

        outboxRepository.save(outbox);
    }

}
