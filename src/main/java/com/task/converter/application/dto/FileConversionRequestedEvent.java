package com.task.converter.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record FileConversionRequestedEvent(
        UUID eventId,
        String bucket,
        String objectKey
) {
}
