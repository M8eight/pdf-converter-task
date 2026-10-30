package com.task.converter.infrastructure.config;

import com.task.converter.infrastructure.config.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3Config {

    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.access-key}")
    private String accessKey;

    @Value("${app.s3.secret-key}")
    private String secretKey;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    private final S3Properties props;

    @Bean
    public S3Client s3Client() {
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        S3Client s3client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of("ru-central1"))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(props.s3CallTimeout()))
                        .apiCallAttemptTimeout(Duration.ofSeconds(props.s3CallAttemptTimeout()))
                        .build())
                .serviceConfiguration(s3Configuration)
                .build();

        try {
            s3client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Бакет {} успешно создан", bucketName);
        }

        return s3client;
    }

}
