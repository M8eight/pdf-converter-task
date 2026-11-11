package com.task.converter.infrastructure.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class JpgToPdfConverter extends AbstractImageToPdfConverter {
    @Override
    public boolean supports(String mimeType) {
        return "image/jpeg".equals(mimeType);
    }

    @Override
    protected PDImageXObject loadImage(PDDocument document, Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return JPEGFactory.createFromStream(document, in);
        }
    }
}
