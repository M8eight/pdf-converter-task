package com.task.converter.application.dto;

import com.task.converter.application.exception.ErrorCode;
import lombok.Builder;

import java.util.UUID;

@Builder
public record FileConvertedEvent(
        UUID eventId,
        UUID correlationId,
        String bucket,
        String objectKey,
        ErrorCode errorCode,
        String errorMessage
) {
}
