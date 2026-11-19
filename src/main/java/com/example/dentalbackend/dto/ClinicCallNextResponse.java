package com.example.dentalbackend.dto;

import java.util.List;
import java.util.UUID;

public record ClinicCallNextResponse(
        UUID sessionId,
        StaffQueueBookingResponse completedBooking,
        StaffQueueBookingResponse servingBooking,
        boolean queueEmpty,
        List<StaffQueueBookingResponse> queue
) {
}
