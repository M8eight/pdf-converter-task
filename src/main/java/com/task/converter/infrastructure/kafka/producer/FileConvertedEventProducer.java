package com.task.converter.infrastructure.kafka.producer;

import com.task.converter.application.dto.FileConvertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileConvertedEventProducer {

    @Value("${app.kafka.topics.output}")
    private String outputTopic;

    private final KafkaTemplate<String, FileConvertedEvent> kafkaTemplate;

    public void sendSync(FileConvertedEvent payload) throws Exception {
        String eventId = payload.eventId().toString();
        kafkaTemplate.send(outputTopic, eventId, payload)
                .get(5, TimeUnit.SECONDS);
        log.info("Сообщение отправлено: {}", eventId);
    }

}
