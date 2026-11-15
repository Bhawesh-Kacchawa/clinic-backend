package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingState;

import java.time.Instant;
import java.util.UUID;

public record StaffQueueBookingResponse(
        UUID id,
        int tokenNo,
        int queueOrder,
        String patientName,
        String phone,
        String reason,
        BookingState status,
        Instant calledAt,
        Instant startedAt,
        Instant completedAt
) {
}
