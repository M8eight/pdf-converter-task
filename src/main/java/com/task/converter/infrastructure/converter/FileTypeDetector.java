package com.task.converter.infrastructure.converter;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class FileTypeDetector {

    private final Tika tika = new Tika();

    public String detect(Path path) throws IOException {
        return tika.detect(path);
    }
}
