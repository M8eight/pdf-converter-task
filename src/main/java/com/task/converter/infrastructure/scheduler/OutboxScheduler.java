package com.task.converter.infrastructure.scheduler;

import com.task.converter.application.dto.FileConvertedEvent;
import com.task.converter.application.service.OutboxService;
import com.task.converter.infrastructure.kafka.producer.FileConvertedEventProducer;
import com.task.converter.infrastructure.persistence.entity.Outbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final ObjectMapper objectMapper;
    private final FileConvertedEventProducer convertedEventProducer;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 500)
    public void process() {
        UUID lockedToken = UUID.randomUUID();
        List<Outbox> pendingEvents = outboxService.claimBatch(lockedToken, 50);

        for (Outbox outbox : pendingEvents) {
            try {
                FileConvertedEvent eventDto = objectMapper
                        .treeToValue(outbox.getPayload(), FileConvertedEvent.class);
                convertedEventProducer.sendSync(eventDto);

                outboxService.setSuccess(outbox.getId(), lockedToken);
            } catch (Exception e) {
                log.error("Ошибка публикации Outbox id={}",  outbox.getId(), e);
                outboxService.retryOrFail(outbox, e.getMessage(), lockedToken);
            }
        }
    }

}
