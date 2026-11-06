package com.task.converter.infrastructure.config;

import com.task.converter.application.dto.FileConversionRequestedEvent;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.exception.EventBusyException;
import com.task.converter.application.service.ConversionResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import static com.task.converter.application.enums.ErrorCode.INTERNAL_ERROR;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Value("${app.inbox.lease-seconds}")
    int inboxLeaseSeconds;

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template,
                                           ConversionResultService conversionResultService) {
        var deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(template);
        deadLetterPublishingRecoverer.setFailIfSendResultIsError(true);

        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            try {
                if (!(cause instanceof EventBusyException)
                        && record.value() instanceof FileConversionRequestedEvent event) {
                    conversionResultService.failAbandoned(event, INTERNAL_ERROR, cause.getMessage());
                }
                deadLetterPublishingRecoverer.accept(record, ex);
            } catch (Exception exception) {
                log.error("Не удалось поставить FAILED для {}", record.key(), exception);
                throw exception;
            }
        };

        var errorHandler = handleBackoff(recoverer);
        errorHandler.addNotRetryableExceptions(ConversionException.class);

        return errorHandler;
    }

    private DefaultErrorHandler handleBackoff(ConsumerRecordRecoverer recoverer) {

        //Суммарная задержка ретраев (75s) 5+10+20+40
        var backoff = new ExponentialBackOffWithMaxRetries(4);
        backoff.setInitialInterval(5_000);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(40_000);

        //Задержка для BUSY статуса, ждем конца lease для проверки
        long busyAttempts = (inboxLeaseSeconds / 10) + 3;
        var busyBackoff = new FixedBackOff(10_000L, busyAttempts);

        var errorHandler = new DefaultErrorHandler(recoverer, backoff);
        errorHandler.setBackOffFunction((record, ex) -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return cause instanceof EventBusyException ? busyBackoff : backoff;
        });

        return errorHandler;
    }

}
