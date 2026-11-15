package com.example.dentalbackend.exception;

public class DuplicateClinicSessionException extends RuntimeException {

    public DuplicateClinicSessionException() {
        super("A session of this type already exists for this clinic and date.");
    }
}
