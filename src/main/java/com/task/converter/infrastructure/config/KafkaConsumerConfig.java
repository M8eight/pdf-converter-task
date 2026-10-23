package com.task.converter.infrastructure.config;

import com.task.converter.application.exception.ConversionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);
        var backoff = new FixedBackOff(1000L, 3);
        var errorHandler = new DefaultErrorHandler(recoverer, backoff);
        errorHandler.addNotRetryableExceptions(ConversionException.class);

        return errorHandler;
    }

}
