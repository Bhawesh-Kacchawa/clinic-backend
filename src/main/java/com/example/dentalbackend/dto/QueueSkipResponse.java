package com.example.dentalbackend.dto;

public record QueueSkipResponse(
        StaffQueueBookingResponse skippedBooking,
        StaffQueueBookingResponse servingBooking
) {
}
