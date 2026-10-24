package com.task.converter.infrastructure.converter;

import com.task.converter.application.service.PdfConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class TxtToPdfConverter implements PdfConverter {

    @Value("${app.pdf.font}")
    private String fontPath;

    private static final float FONT_SIZE = 11;
    private static final float LEADING = 14;
    private static final float MARGIN = 50;
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MAX_WIDTH = PAGE_SIZE.getWidth() - 2 * MARGIN;

    @Override
    public boolean supports(String mimeType) {
        return "text/plain".equals(mimeType);
    }

    @Override
    public Path convert(Path path) throws IOException {

        Path resPdf = Files.createTempFile("converted-", ".pdf");

        try (PDDocument document = new PDDocument();
             BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

            PDType0Font font = loadFont(document);
            renderTxtToPdf(reader, font, document);

            document.save(resPdf.toFile());
        }
        return resPdf;
    }

    private void renderTxtToPdf(BufferedReader reader, PDFont font, PDDocument document) throws IOException {
        PDPageContentStream stream = null;
        float y = 0;

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String part : wrap(line.replace("\t", "    ")
                        .replace("\r", ""), font)) {

                    if (stream == null || y < MARGIN) {
                        if (stream != null) {
                            stream.close();
                        }
                        PDPage page = addNewPage(document);
                        stream = new PDPageContentStream(document, page);
                        stream.setFont(font, FONT_SIZE);
                        y = PAGE_SIZE.getHeight() - MARGIN;
                    }

                    stream.beginText();
                    stream.newLineAtOffset(MARGIN, y);
                    stream.showText(part);
                    stream.endText();
                    y -= LEADING;
                }
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        if (document.getNumberOfPages() == 0) {
            document.addPage(new PDPage(PAGE_SIZE));
        }
    }

    private PDPage addNewPage(PDDocument document) {
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);
        return page;
    }

    private PDType0Font loadFont(PDDocument document) throws IOException {
        try (InputStream fontStream = getClass().getResourceAsStream(fontPath)) {
            if (fontStream == null) {
                throw new FileNotFoundException("Font file not found %s".formatted(fontPath));
            }
            return PDType0Font.load(document, fontStream);
        }
    }

    private List<String> wrap(String text, PDFont font) throws IOException {
        List<String> lines = new ArrayList<>();
        String cur = "";

        for (String word : text.split(" ", -1)) {
            String candidateLine = cur.isEmpty() ? word : cur + " " + word;
            if (!cur.isEmpty() && widthOf(candidateLine, font) > MAX_WIDTH) {
                lines.add(cur);
                cur = word;
            } else {
                cur = candidateLine;
            }
        }
        lines.add(cur);
        return lines;
    }

    private float widthOf(String text, PDFont font) throws IOException {
        return font.getStringWidth(text) * FONT_SIZE / 1000f;
    }

}
