package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.SessionType;

import java.time.Instant;
import java.time.LocalTime;

public record PublicSessionDto(
        SessionType type,
        LocalTime startTime,
        LocalTime endTime,
        int maxAppointments,
        long bookedCount,
        BookingStatus bookingStatus,
        Instant bookingOpensAt
) {
}
