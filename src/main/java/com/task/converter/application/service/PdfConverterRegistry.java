package com.task.converter.application.service;

import com.task.converter.application.exception.ConversionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.task.converter.application.exception.ErrorCode.UNSUPPORTED_FILE_TYPE;

@Component
@RequiredArgsConstructor
public class PdfConverterRegistry {

    private final List<PdfConverter> converters;

    public PdfConverter get(String mime) {
        return converters.stream()
                .filter(c -> c.supports(mime))
                .findFirst()
                .orElseThrow(() -> new ConversionException("Unsupported file type", UNSUPPORTED_FILE_TYPE));
    }

}
