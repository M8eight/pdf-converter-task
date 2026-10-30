package com.task.converter.infrastructure.config;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.service.ConversionResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import static com.task.converter.application.exception.ErrorCode.INTERNAL_ERROR;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template,
                                           ConversionResultService conversionResultService) {
        var deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(template);
        deadLetterPublishingRecoverer.setFailIfSendResultIsError(true);
        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            try {
                if (record.value() instanceof RequestedFileEvent event) {
                    conversionResultService.completeFailed(event, INTERNAL_ERROR, ex.getMessage());
                }
            } catch (Exception inboxEx) {
                log.error("Не удалось поставить FAILED для {}", record.key(), inboxEx);
                throw inboxEx;
            }
            deadLetterPublishingRecoverer.accept(record, ex);
        };
        //Суммарная задержка 75s
        var backoff = new ExponentialBackOffWithMaxRetries(4);
        backoff.setInitialInterval(5_000);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(40_000);

        var errorHandler = new DefaultErrorHandler(recoverer, backoff);
        errorHandler.addNotRetryableExceptions(ConversionException.class);

        return errorHandler;
    }

}
