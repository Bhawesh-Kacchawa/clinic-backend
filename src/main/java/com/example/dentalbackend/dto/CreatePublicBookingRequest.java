package com.example.dentalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePublicBookingRequest(
        @NotBlank @Size(max = 36) String sessionId,
        @NotBlank @Size(max = 150) String patientName,
        @NotBlank @Size(max = 30) String phone,
        @Size(max = 1000) String reason
) {
}
