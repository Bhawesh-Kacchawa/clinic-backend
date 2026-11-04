package com.example.dentalbackend.dto;

import java.time.Instant;
import java.util.List;

public record PublicSessionsResponse(Instant serverTime, List<PublicSessionDto> sessions) {
}
