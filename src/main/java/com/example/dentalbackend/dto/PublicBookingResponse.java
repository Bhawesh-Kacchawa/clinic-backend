package com.example.dentalbackend.dto;

import java.util.UUID;

public record PublicBookingResponse(UUID bookingId, int tokenNo, BookingSessionResponse session) {
}
