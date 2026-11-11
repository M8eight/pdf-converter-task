package com.task.converter.application.exception;

public class EventBusyException extends RuntimeException {
    public EventBusyException(String message) {
        super(message);
    }
}
