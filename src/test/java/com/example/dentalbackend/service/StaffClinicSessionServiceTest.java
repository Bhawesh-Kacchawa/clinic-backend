package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.UpdateClinicSessionStatusRequest.BookingAction;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffClinicSessionServiceTest {

    @Test
    void opensTodaysMorningAfterNoonAndClearsOverrideOnPause() {
        UUID clinicId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 25);
        ClinicSession session = new ClinicSession();
        session.setId(UUID.randomUUID());
        session.setDoctor(new StaffUser());
        session.setDate(date);
        session.setType(SessionType.MORNING);
        session.setBookingStatus(BookingStatus.OPEN);
        session.setBookingOpensAt(Instant.parse("2026-11-25T07:30:00Z"));
        ClinicSessionRepository repository = mock(ClinicSessionRepository.class);
        when(repository.findByClinicIdAndSlugAndDateAndTypeForUpdate(clinicId, "clinic", date, SessionType.MORNING))
                .thenReturn(Optional.of(session));
        when(repository.save(any(ClinicSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StaffClinicSessionService service = service(repository, Instant.parse("2026-11-25T06:30:00Z"));

        assertEquals(BookingStatus.OPEN,
                service.updateStatusForToday(clinicId, "clinic", SessionType.MORNING, BookingAction.OPEN).bookingStatus());
        assertTrue(session.isMorningBookingOverride());
        assertEquals(Instant.parse("2026-11-25T06:30:00Z"), session.getBookingOpensAt());
        assertEquals(BookingStatus.PAUSED,
                service.updateStatusForToday(clinicId, "clinic", SessionType.MORNING, BookingAction.PAUSE).bookingStatus());
        assertFalse(session.isMorningBookingOverride());
        assertThrows(NotFoundException.class,
                () -> service.updateStatusForToday(UUID.randomUUID(), "clinic", SessionType.MORNING, BookingAction.OPEN));
    }

    @Test
    void openingBeforeNoonDoesNotOverrideAutomaticCutoff() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClinicSession session = new ClinicSession();
        session.setDoctor(new StaffUser());
        session.setDate(LocalDate.of(2026, 11, 25));
        session.setType(SessionType.MORNING);
        session.setBookingOpensAt(Instant.EPOCH);
        ClinicSessionRepository repository = mock(ClinicSessionRepository.class);
        when(repository.findByIdAndClinic_IdForUpdate(sessionId, clinicId)).thenReturn(Optional.of(session));
        when(repository.save(any(ClinicSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StaffClinicSessionService service = service(repository, Instant.parse("2026-11-25T06:29:59Z"));

        service.updateStatus(clinicId, sessionId, BookingAction.OPEN);
        assertFalse(session.isMorningBookingOverride());
        StaffClinicSessionService afternoonService = service(repository, Instant.parse("2026-11-25T06:30:00Z"));
        afternoonService.updateStatus(clinicId, sessionId, BookingAction.OPEN);
        assertTrue(session.isMorningBookingOverride());
        afternoonService.updateStatus(clinicId, sessionId, BookingAction.CLOSE);
        assertEquals(BookingStatus.CLOSED, session.getBookingStatus());
        assertFalse(session.isMorningBookingOverride());
    }

    private StaffClinicSessionService service(ClinicSessionRepository repository, Instant now) {
        return new StaffClinicSessionService(
                repository,
                mock(ClinicRepository.class),
                mock(StaffUserRepository.class),
                mock(BookingRepository.class),
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }
}
