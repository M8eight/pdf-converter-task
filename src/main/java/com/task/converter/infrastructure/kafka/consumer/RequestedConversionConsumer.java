package com.task.converter.infrastructure.kafka.consumer;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.exception.EventBusyException;
import com.task.converter.application.service.ConversionResultService;
import com.task.converter.application.service.FileConversionService;
import com.task.converter.application.service.InboxService;
import com.task.converter.application.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static com.task.converter.application.enums.ConversionStatus.FAILED;
import static com.task.converter.application.enums.ConversionStatus.PENDING;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestedConversionConsumer {

    private final FileConversionService fileConversionService;
    private final InboxService inboxService;
    private final ConversionResultService conversionResultService;

    @KafkaListener(topics = "${app.kafka.topics.input}", groupId = "pdf-service")
    public void listen(RequestedFileEvent event) throws IOException {

        UUID lockedToken = UUID.randomUUID();
        switch (inboxService.claim(event.eventId(), lockedToken)) {
            case CLAIMED -> {}
            case BUSY -> throw new EventBusyException(event.eventId().toString());
            case ALREADY_DONE -> { return; }
        }

        try {
            String filename = fileConversionService.processConversation(event);
            conversionResultService.completeSuccessfully(event, filename, lockedToken);
        } catch (ConversionException e) {
            conversionResultService.completeFailed(event, e.code(), e.getMessage());
        } catch (Exception e) {
            inboxService.setPending(event.eventId(), lockedToken);
            throw e;
        }
    }

}
