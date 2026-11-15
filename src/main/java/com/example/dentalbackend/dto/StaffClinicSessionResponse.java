package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.SessionType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record StaffClinicSessionResponse(
        UUID id,
        UUID doctorId,
        SessionType type,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int maxAppointments,
        int avgMinutes,
        Instant bookingOpensAt,
        BookingStatus bookingStatus
) {
}
