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

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

    private final InboxRepository inboxRepository;

    @Value("${app.timeouts.inbox.lease-seconds}")
    private long leaseSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claim(UUID eventId, UUID lockedToken) {
        if (inboxRepository.claim(eventId, leaseSeconds, lockedToken) == 1) {
            log.info("CLAIMED");
            return ClaimResult.CLAIMED;
        }
        return inboxRepository.findById(eventId)
                .map(Inbox::getStatus)
                .filter(s -> s == ConversionStatus.SUCCESS || s == ConversionStatus.FAILED)
                .map(s -> ClaimResult.ALREADY_DONE)
                .orElse(ClaimResult.BUSY);
    }

    @Transactional
    public void setSuccess(UUID eventId, UUID lockToken) {
        log.info("SUCCESS");
        int updated = inboxRepository.setSuccessIfProcessing(eventId, lockToken);
        checkIsUpdated(eventId, updated, ConversionStatus.SUCCESS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setFailed(UUID eventId) {
        log.info("FAILED");
        int updated = inboxRepository.setFailedIfNotTerminal(eventId);
        checkIsUpdated(eventId, updated, ConversionStatus.FAILED);
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
