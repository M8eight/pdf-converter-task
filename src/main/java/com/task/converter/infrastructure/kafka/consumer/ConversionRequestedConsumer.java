package com.task.converter.infrastructure.kafka.consumer;

import com.task.converter.application.dto.FileConversionRequestedEvent;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.enums.ErrorCode;
import com.task.converter.application.exception.EventBusyException;
import com.task.converter.application.service.ConversionResultService;
import com.task.converter.application.service.FileConversionService;
import com.task.converter.application.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConversionRequestedConsumer {

    private final FileConversionService fileConversionService;
    private final InboxService inboxService;
    private final ConversionResultService conversionResultService;

    @KafkaListener(topics = "${app.kafka.topics.input}", groupId = "pdf-service")
    public void listen(FileConversionRequestedEvent event) throws IOException {

        UUID lockedToken = UUID.randomUUID();
        switch (inboxService.claim(event.eventId(), lockedToken)) {
            case CLAIMED -> {}
            case BUSY -> throw new EventBusyException(event.eventId().toString());
            case ALREADY_DONE -> { return; }
            case EXHAUSTED -> {
                conversionResultService.failAbandoned(event, ErrorCode.INTERNAL_ERROR,
                        "Превышено число попыток обработки");
                return;
            }
        }

        try {
            fileConversionService.processConversation(event);
            conversionResultService.completeSuccessfully(event, lockedToken);
        } catch (ConversionException e) {
            conversionResultService.completeFailed(event, e.code(), e.getMessage(), lockedToken);
        } catch (Exception e) {
            inboxService.setPending(event.eventId(), lockedToken);
            throw e;
        }
    }

}
