package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicBookingDto(
        UUID bookingId,
        int tokenNo,
        String patientName,
        String phone,
        BookingState status,
        SessionType sessionType,
        LocalDate sessionDate,
        LocalTime sessionStartTime,
        LocalTime sessionEndTime
) {
}
