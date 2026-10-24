package com.task.converter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.files")
public record FilesProperties(
        DataSize maxFileSize,
        DataSize maxUnpackedSize,
        int maxArchiveEntries
) {
}
