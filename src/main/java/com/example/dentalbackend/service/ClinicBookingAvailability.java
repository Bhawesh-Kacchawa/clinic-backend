package com.example.dentalbackend.service;

import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.SessionType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

final class ClinicBookingAvailability {

    private static final ZoneId CLINIC_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private ClinicBookingAvailability() {
    }

    static BookingStatus statusAt(
            SessionType type,
            LocalDate date,
            BookingStatus currentStatus,
            Instant now
    ) {
        if (type == SessionType.MORNING
                && !now.atZone(CLINIC_TIME_ZONE).isBefore(date.atTime(LocalTime.NOON).atZone(CLINIC_TIME_ZONE))) {
            return BookingStatus.CLOSED;
        }
        return currentStatus;
    }
}
