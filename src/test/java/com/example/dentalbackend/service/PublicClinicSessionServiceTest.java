package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.PublicSessionDto;
import com.example.dentalbackend.dto.PublicSessionsResponse;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicClinicSessionServiceTest {

    private static final ZoneId CLINIC_TIME_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Test
    void showsBothSessionsOpenBeforeNoonAndOnlyEveningOpenAtNoon() {
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic();
        clinic.setId(clinicId);
        ClinicRepository clinicRepository = mock(ClinicRepository.class);
        ClinicSessionRepository sessionRepository = mock(ClinicSessionRepository.class);
        when(clinicRepository.findBySlug("clinic")).thenReturn(Optional.of(clinic));
        when(sessionRepository.findPublicSessions(clinicId, TODAY)).thenReturn(List.of(
                session(SessionType.MORNING),
                session(SessionType.EVENING)
        ));

        PublicClinicSessionService beforeNoon = new PublicClinicSessionService(
                clinicRepository,
                sessionRepository,
                Clock.fixed(TODAY.atTime(11, 59).atZone(CLINIC_TIME_ZONE).toInstant(), ZoneId.of("UTC"))
        );
        PublicSessionsResponse beforeNoonResponse = beforeNoon.getTodaysSessions("clinic");
        assertEquals(
                List.of(BookingStatus.OPEN, BookingStatus.OPEN),
                beforeNoonResponse.sessions().stream().map(PublicSessionDto::bookingStatus).toList()
        );

        PublicClinicSessionService atNoon = new PublicClinicSessionService(
                clinicRepository,
                sessionRepository,
                Clock.fixed(TODAY.atTime(12, 0).atZone(CLINIC_TIME_ZONE).toInstant(), ZoneId.of("UTC"))
        );
        PublicSessionsResponse atNoonResponse = atNoon.getTodaysSessions("clinic");
        assertEquals(
                List.of(BookingStatus.CLOSED, BookingStatus.OPEN),
                atNoonResponse.sessions().stream().map(PublicSessionDto::bookingStatus).toList()
        );
    }

    private PublicSessionDto session(SessionType type) {
        return new PublicSessionDto(
                UUID.randomUUID(),
                type,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                10,
                0,
                BookingStatus.OPEN,
                Instant.EPOCH
        );
    }
}
