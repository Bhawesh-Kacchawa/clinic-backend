package com.example.dentalbackend.dto;

public record CallNextResponse(
        StaffQueueBookingResponse completedBooking,
        StaffQueueBookingResponse servingBooking,
        boolean queueEmpty
) {
}
