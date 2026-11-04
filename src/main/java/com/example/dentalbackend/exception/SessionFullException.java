package com.example.dentalbackend.exception;

public class SessionFullException extends RuntimeException {

    public SessionFullException(String message) {
        super(message);
    }
}
