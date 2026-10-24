package com.task.converter.infrastructure.converter;

import com.task.converter.application.service.PdfConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractImageToPdfConverter implements PdfConverter {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50;

    protected abstract PDImageXObject loadImage(PDDocument document, Path path) throws IOException;

    @Override
    public Path convert(Path path) throws IOException {
        Path resPdf = Files.createTempFile("converted-", ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDImageXObject image = loadImage(document, path);

            float maxW = PAGE_SIZE.getWidth() - 2 * MARGIN;
            float maxH = PAGE_SIZE.getHeight() - 2 * MARGIN;

            float scale = Math.min(1f,
                    Math.min(maxW / image.getWidth(), maxH / image.getHeight())
            );
            float w = image.getWidth() * scale;
            float h = image.getHeight() * scale;

            float x = (PAGE_SIZE.getWidth() - w) / 2;
            float y = (PAGE_SIZE.getHeight() - h) / 2;

            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(image, x, y, w, h);
            }

            document.save(resPdf.toFile());
            return resPdf;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(resPdf);
            throw e;
        }
    }
}
