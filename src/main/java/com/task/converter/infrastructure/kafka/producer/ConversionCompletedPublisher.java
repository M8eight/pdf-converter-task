package com.task.converter.infrastructure.kafka.producer;

import com.task.converter.application.dto.FileConversionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionCompletedPublisher {

    @Value("${app.kafka.topics.output}")
    private String outputTopic;

    private final KafkaTemplate<String, FileConversionCompletedEvent> kafkaTemplate;

    public void sendSync(FileConversionCompletedEvent payload) throws Exception {
        String eventId = payload.eventId().toString();
        try {
            kafkaTemplate.send(outputTopic, eventId, payload)
                    .get(5, TimeUnit.SECONDS);
            log.debug("Отправлено в kafka {}", eventId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interrupted", e.getCause());
        }
    }

}
