package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingState;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicBookingStatusResponse(
        UUID bookingId,
        int token,
        BookingState status,
        Integer currentServingToken,
        long patientsAhead,
        LocalDateTime estimatedTime
) {
}
