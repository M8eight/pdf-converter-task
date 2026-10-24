package com.task.converter.application.service;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.application.exception.ConversionException;
import com.task.converter.infrastructure.converter.FileTypeDetector;
import com.task.converter.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.task.converter.application.enums.ConversionStatus.FAILED;
import static com.task.converter.application.enums.ConversionStatus.PENDING;
import static com.task.converter.application.exception.ErrorCode.INTERNAL_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileConversionService {

    private final StorageService storageService;
    private final PdfConverterRegistry converterRegistry;
    private final FileTypeDetector detector;
    private final OutboxService outboxService;

    public void processConversation(RequestedFileEvent event) throws IOException {
        Path resTmpPdf = null;
        Path tmp = Files.createTempFile("convert-", ".tmp");
        try {

            try (var in = storageService.downloadFile(event.bucket(), event.objectKey())) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            String mime = detector.detect(tmp);
            resTmpPdf = converterRegistry.get(mime).convert(tmp);
            storageService.saveFile(resTmpPdf.getFileName().toString(), resTmpPdf);

            outboxService.saveOutbox(event, resTmpPdf.getFileName().toString(), PENDING, null, null);

        } catch (ConversionException e) {
            log.warn("Перехваченная ошибка конвертации: {}", e.getMessage());
            outboxService.saveOutbox(event, null, FAILED, e.code(), e.getMessage());
        } catch (Exception e) {
            log.error("Неперехваченная ошибка обработки файла", e);
            outboxService.saveOutbox(event, null, FAILED, INTERNAL_ERROR, e.getMessage());
        } finally {
            deleteQuiet(resTmpPdf);
            deleteQuiet(tmp);
        }
    }

    private void deleteQuiet(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Не удалось очистить временный файл {}", path, e);
            }
        }
    }

}
