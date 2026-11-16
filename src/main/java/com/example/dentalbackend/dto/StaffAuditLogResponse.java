package com.example.dentalbackend.dto;

import com.example.dentalbackend.entity.BookingState;

import java.time.Instant;
import java.util.UUID;

public record StaffAuditLogResponse(
        UUID id,
        UUID staffId,
        UUID bookingId,
        Integer tokenNo,
        String action,
        BookingState beforeStatus,
        BookingState afterStatus,
        Instant createdAt
) {
}
