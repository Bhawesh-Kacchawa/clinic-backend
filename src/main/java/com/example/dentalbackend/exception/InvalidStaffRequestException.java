package com.example.dentalbackend.exception;

public class InvalidStaffRequestException extends RuntimeException {

    public InvalidStaffRequestException(String message) {
        super(message);
    }
}
