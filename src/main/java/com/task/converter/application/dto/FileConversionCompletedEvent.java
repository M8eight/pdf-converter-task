package com.task.converter.application.dto;

import com.task.converter.application.enums.ErrorCode;
import lombok.Builder;

import java.util.UUID;

@Builder
public record FileConversionCompletedEvent(
        UUID eventId,
        UUID correlationId,
        String bucket,
        ErrorCode errorCode,
        String errorMessage
) {
}
