package com.example.dentalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateWalkInBookingRequest(
        @NotNull UUID sessionId,
        @NotBlank @Size(max = 150) String patientName,
        @NotBlank @Size(max = 30) String phone,
        @Size(max = 1000) String address
) {
}
