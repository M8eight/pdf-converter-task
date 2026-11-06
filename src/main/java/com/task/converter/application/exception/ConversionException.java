package com.task.converter.application.exception;

import com.task.converter.application.enums.ErrorCode;

public class ConversionException extends RuntimeException {

    private final ErrorCode code;

    public ConversionException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() { return code; }

}
