package com.task.converter.infrastructure.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

@Component
public class PngToPdfConverter extends AbstractImageToPdfConverter {

    private static final long MAX_PIXELS = 50_000_000L;

    public boolean supports(String mime) {
        return "image/png".equals(mime);
    }

    @Override
    protected PDImageXObject loadImage(PDDocument document, Path path) throws IOException {
        checkPixels(path);
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Cannot decode png");
        }
        return LosslessFactory.createFromImage(document, image);
    }

    private void checkPixels(Path path) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IOException("Cannot decode png");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new IOException("Image too large %s pixels".formatted(pixels));
                }
            } finally {
                reader.dispose();
            }
        }
    }

}
