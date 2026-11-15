package com.example.dentalbackend.exception;

public class QueueStateConflictException extends RuntimeException {

    public QueueStateConflictException(String message) {
        super(message);
    }
}
