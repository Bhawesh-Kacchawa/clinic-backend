package com.example.dentalbackend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UndoQueueActionRequest(@NotNull UUID bookingId) {
}
