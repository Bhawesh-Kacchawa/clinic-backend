package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.SessionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateClinicSessionRequest(
        @NotNull SessionType type,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(1) int maxAppointments,
        @Min(1) int avgMinutes,
        @NotNull Instant bookingOpensAt
) {
}
