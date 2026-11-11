package com.task.converter.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.converter.application.dto.FileConversionCompletedEvent;
import com.task.converter.application.service.OutboxService;
import com.task.converter.infrastructure.kafka.producer.ConversionCompletedPublisher;
import com.task.converter.infrastructure.persistence.entity.Outbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final ObjectMapper objectMapper;
    private final ConversionCompletedPublisher convertedEventProducer;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 500)
    public void process() {
        UUID lockedToken = UUID.randomUUID();
        List<Outbox> pendingEvents = outboxService.claimBatch(lockedToken, 50);

        for (Outbox outbox : pendingEvents) {
            try {
                FileConversionCompletedEvent eventDto = objectMapper
                        .treeToValue(outbox.getPayload(), FileConversionCompletedEvent.class);
                convertedEventProducer.sendSync(eventDto);

                outboxService.setSuccess(outbox.getId(), lockedToken);
                log.info("SUCCESS");
            } catch (Exception e) {
                log.error("Ошибка публикации Outbox id={}",  outbox.getId(), e);
                outboxService.retryOrFail(outbox, e.getMessage(), lockedToken);
                log.info("FAILED");
            }
        }
    }

}
