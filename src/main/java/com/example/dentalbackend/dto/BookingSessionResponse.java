package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingSessionResponse(
        UUID sessionId,
        SessionType type,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
}
