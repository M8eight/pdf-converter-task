package com.task.converter.infrastructure.scheduler;

import com.task.converter.application.dto.FileConvertedEvent;
import com.task.converter.application.service.OutboxService;
import com.task.converter.infrastructure.kafka.producer.FileConvertedEventProducer;
import com.task.converter.infrastructure.persistence.entity.Outbox;
import com.task.converter.infrastructure.persistence.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final FileConvertedEventProducer convertedEventProducer;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void process() {

        List<Outbox> pendingEvents = outboxService.findPendingEvents(50);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (Outbox outbox : pendingEvents) {
            try {

                FileConvertedEvent eventDto = objectMapper
                        .treeToValue(outbox.getPayload(), FileConvertedEvent.class);
                convertedEventProducer.sendSync(eventDto);

                outboxService.setSuccess(outbox.getId());

            } catch (Exception e) {
                log.error("Ошибка при обработке outbox {}", outbox.getId(), e);
                outboxService.setFailed(outbox.getId());
            }
        }
    }

}
