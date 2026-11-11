package com.task.converter.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${app.files.max-file-size}")
    private DataSize maxFileSize;

    @Value("${app.s3.folders.converted}")
    private String convertedFolder;

    public void saveFile(UUID fileKey, Path path) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(createObjectKey(fileKey))
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromFile(path));
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String bucket, String objectKey) {

        HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());

        if (head.contentLength() > maxFileSize.toBytes()) {
            throw new RuntimeException("File is too large");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    public String createObjectKey(UUID fileKey) {
        return convertedFolder + fileKey + ".pdf";
    }

}
