package com.task.converter.application.service;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionResultService {

    private final InboxService inboxService;
    private final OutboxService outboxService;

    @Transactional
    public void completeSuccessfully(RequestedFileEvent event, String filename, UUID inboxLockedToken) {
        inboxService.setSuccess(event.eventId(), inboxLockedToken);
        outboxService.createOutbox(event, filename, null, null);
    }

    @Transactional
    public void completeFailed(RequestedFileEvent event, ErrorCode errorCode,
                               String errorMessage, UUID inboxLockedToken) {
        inboxService.setFailed(event.eventId(), inboxLockedToken);
        outboxService.createOutbox(event, null, errorCode, errorMessage);
    }

    @Transactional
    public void failAbandoned(RequestedFileEvent event, ErrorCode errorCode,
                               String errorMessage) {
        if (inboxService.setFailedIfAbandoned(event.eventId())) {
            outboxService.createOutbox(event, null, errorCode, errorMessage);
        } else {
            log.warn("Inbox {} уже FAILED или обрабатывается", event.eventId());
        }
    }

}
