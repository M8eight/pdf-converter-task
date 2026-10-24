package com.task.converter.application.exception;

public class ConversionException extends RuntimeException {

    private final ErrorCode code;

    public ConversionException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() { return code; }

}
