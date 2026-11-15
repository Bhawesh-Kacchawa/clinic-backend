package com.example.dentalbackend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

public record UpdateClinicSessionStatusRequest(@NotNull BookingAction action) {

    public enum BookingAction {
        OPEN,
        PAUSE,
        CLOSE;

        @JsonCreator
        public static BookingAction fromValue(String value) {
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "OPEN" -> OPEN;
                case "PAUSE", "PAUSED" -> PAUSE;
                case "CLOSE", "CLOSED" -> CLOSE;
                default -> throw new IllegalArgumentException("Action must be Open, Pause, or Close.");
            };
        }
    }
}
