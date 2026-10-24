package com.task.converter.application.service;

import java.io.IOException;
import java.nio.file.Path;

public interface PdfConverter {
    boolean supports(String mimeType);
    Path convert(Path path) throws IOException;
}
