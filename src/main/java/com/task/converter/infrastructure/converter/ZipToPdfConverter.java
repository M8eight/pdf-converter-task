package com.task.converter.infrastructure.converter;

import com.task.converter.application.exception.ConversionException;
import com.task.converter.application.service.PdfConverter;
import com.task.converter.application.service.PdfConverterRegistry;
import com.task.converter.infrastructure.config.properties.FilesProperties;
import com.task.converter.infrastructure.storage.TempFiles;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static com.task.converter.application.exception.ErrorCode.*;

@Component
@RequiredArgsConstructor
public class ZipToPdfConverter implements PdfConverter {

    private static final Charset FALLBACK_CHARSET = Charset.forName("Cp866");

    private final ObjectProvider<PdfConverterRegistry> registry;
    private final FileTypeDetector detector;
    private final FilesProperties props;

    @Override
    public boolean supports(String mimeType) {
        return "application/zip".equals(mimeType);
    }

    @Override
    public Path convert(Path path) throws IOException {
        try (TempFiles temp = new TempFiles(); ZipFile zip = openZip(path)) {
            List<Path> parts = convertEntries(zip, temp);
            if (parts.isEmpty()) {
                throw new ConversionException("Archive contains no files", EMPTY_ARCHIVE);
            }
            return merge(parts);
        } catch (ZipException e) {
            throw new ConversionException("Cannot read archive", CORRUPTED_ARCHIVE);
        }
    }

    private List<Path> convertEntries(ZipFile zip, TempFiles temp) throws IOException {
        PdfConverterRegistry converters = registry.getObject();
        List<Path> parts = new ArrayList<>();
        long remaining = props.maxUnpackedSize().toBytes();
        int count = 0;

        for (ZipEntry entry : Collections.list(zip.entries())) {
            if (isSkippable(entry)) {
                continue;
            }
            if (++count > props.maxArchiveEntries()) {
                throw new ConversionException("Too many files in archive", TOO_MANY_FILES);
            }

            Path unpacked = temp.create("zip-entry-", ".tmp");
            remaining -= unpack(zip, entry, unpacked, remaining);

            String mime = detector.detect(unpacked);
            parts.add(temp.track(converters.get(mime).convert(unpacked)));

            Files.deleteIfExists(unpacked);
        }
        return parts;
    }

    private Path  merge(List<Path> parts) throws IOException {
        Path result = Files.createTempFile("converted-", ".pdf");
        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (Path part : parts) {
                merger.addSource(part.toFile());
            }
            merger.setDestinationFileName(result.toString());
            merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
            return result;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(result);
            throw e;
        }
    }

    private long unpack(ZipFile zip, ZipEntry entry, Path target, long limit) throws IOException {
        long written = 0;
        try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                written += n;
                if (written > limit) {
                    throw new ConversionException("Archive is too large after unpacking", ARCHIVE_TOO_LARGE);
                }
                out.write(buffer, 0, n);
            }
        }
        return written;
    }

    private ZipFile openZip(Path path) throws IOException {
        try {
            return new ZipFile(path.toFile(), StandardCharsets.UTF_8);
        } catch (ZipException e) {
            return new ZipFile(path.toFile(), FALLBACK_CHARSET);
        }
    }



    private boolean isSkippable(ZipEntry entry) {
        String name = entry.getName();
        return entry.isDirectory()
                || name.startsWith("__MACOSX/")
                || shortName(name).equals(".DS_Store");
    }

    private String shortName(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

}
