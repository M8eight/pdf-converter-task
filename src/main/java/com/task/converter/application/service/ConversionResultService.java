package com.task.converter.application.service;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.enums.ConversionStatus;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.task.converter.application.enums.ConversionStatus.FAILED;
import static com.task.converter.application.enums.ConversionStatus.PENDING;

@Service
@RequiredArgsConstructor
public class ConversionResultService {

    private final InboxService inboxService;
    private final OutboxService outboxService;

    @Transactional
    public void completeSuccessfully(RequestedFileEvent event, String filename, UUID inboxLockedToken) {
        inboxService.setSuccess(event.eventId(), inboxLockedToken);
        outboxService.createOutbox(event, filename,
                PENDING, null, null);
    }

    @Transactional
    public void completeFailed(RequestedFileEvent event, ErrorCode errorCode, String errorMessage) {
        inboxService.setFailed(event.eventId());
        outboxService.createOutbox(event, null, FAILED, errorCode, errorMessage);
    }

}
