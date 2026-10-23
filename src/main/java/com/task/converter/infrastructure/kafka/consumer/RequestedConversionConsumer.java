package com.task.converter.infrastructure.kafka.consumer;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.service.FileConversionService;
import com.task.converter.application.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestedConversionConsumer {

    private final FileConversionService fileConversionService;
    private final InboxService inboxService;

    @KafkaListener(topics = "${app.kafka.topics.input}", groupId = "pdf-service")
    public void listen(RequestedFileEvent event) throws IOException {
        int inserted = inboxService.getInserted(event);

        if (inserted == 0) {
            return;
        }

        try {
            fileConversionService.processConversation(event);
            log.info("Успешно обработано: {}", event);
        } catch (Exception e) {
            log.error("Ошибка при обработке файла", e);
            inboxService.remove(event.eventId());
            throw e;
        }
    }

}
