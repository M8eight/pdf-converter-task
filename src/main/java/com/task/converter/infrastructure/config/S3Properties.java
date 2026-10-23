package com.task.converter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.timeouts.s3")
public record S3Properties(
        Integer s3CallTimeout,
        Integer s3CallAttemptTimeout
) {
}
