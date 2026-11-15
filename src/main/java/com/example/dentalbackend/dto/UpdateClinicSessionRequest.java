package com.example.dentalbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record UpdateClinicSessionRequest(
        @NotNull UUID id,
        LocalTime startTime,
        LocalTime endTime,
        @Min(1) Integer maxAppointments,
        @Min(1) Integer avgMinutes,
        Instant bookingOpensAt
) {
}
