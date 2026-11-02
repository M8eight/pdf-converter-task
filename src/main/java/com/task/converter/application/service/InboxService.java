package com.task.converter.application.service;

import com.task.converter.application.enums.ClaimResult;
import com.task.converter.application.enums.ConversionStatus;
import com.task.converter.infrastructure.persistence.entity.Inbox;
import com.task.converter.infrastructure.persistence.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.task.converter.application.enums.ClaimResult.*;
import static com.task.converter.application.enums.ConversionStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

    private final InboxRepository inboxRepository;

    @Value("${app.inbox.lease-seconds}")
    private long leaseSeconds;

    @Value("${app.inbox.max-attempts}")
    private int maxAttempts;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claim(UUID eventId, UUID lockedToken) {
        if (inboxRepository.claim(eventId, leaseSeconds, lockedToken, maxAttempts) == 1) {
            return ClaimResult.CLAIMED;
        }

        Inbox inbox = inboxRepository.findById(eventId).orElseThrow();
        if (inbox.getStatus() == SUCCESS || inbox.getStatus() == FAILED) {
            return ALREADY_DONE;
        }

        if (inbox.getStatus() == PROCESSING
                && inbox.getLockedUntil() != null
                && inbox.getLockedUntil().isAfter(Instant.now())) {
            return BUSY;
        }

        if (inbox.getRetryAttempts() >= maxAttempts) {
            return EXHAUSTED;
        }

        return BUSY;
    }

    @Transactional
    public void setSuccess(UUID eventId, UUID lockToken) {
        log.info("SUCCESS");
        int updated = inboxRepository.setSuccessIfProcessing(eventId, lockToken);
        checkIsUpdated(eventId, updated, SUCCESS);
    }

    @Transactional
    public void setFailed(UUID eventId, UUID lockToken) {
        log.info("FAILED");
        int updated = inboxRepository.setFailedIfNotTerminal(eventId, lockToken);
        checkIsUpdated(eventId, updated, ConversionStatus.FAILED);
    }

    @Transactional
    public boolean setFailedIfAbandoned(UUID eventId) {
        log.info("FAILED ABANDON");
        int updated = inboxRepository.setFailedIfAbandoned(eventId);
        checkIsUpdated(eventId, updated, ConversionStatus.FAILED);

        return updated == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setPending(UUID eventId, UUID lockedToken) {
        log.info("PENDING");
        int updated = inboxRepository.setPendingIfProcessing(eventId, lockedToken);
        checkIsUpdated(eventId, updated, ConversionStatus.PENDING);
    }

    private static void checkIsUpdated(UUID eventId, int updated, ConversionStatus status) {
        if (updated == 0) {
            log.warn("Не удалось сменить статус на {} eventId={}", status, eventId);
            throw new IllegalStateException(
                    "Не удалось завершить обработку события: " + eventId
            );
        }
    }

}
