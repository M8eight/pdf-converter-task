package com.task.converter.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TempFiles implements AutoCloseable{

    private final List<Path> files = new ArrayList<>();

    public Path create(String prefix, String suffix) throws IOException {
        return track(Files.createTempFile(prefix, suffix));
    }

    public Path track(Path file) {
        files.add(file);
        return file;
    }

    @Override
    public void close() {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Не удалось удалить временный файл {}", file, e);
            }
        }
    }
}
