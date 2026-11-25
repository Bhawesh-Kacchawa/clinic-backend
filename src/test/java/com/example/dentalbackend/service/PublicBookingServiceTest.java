package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CreatePublicBookingRequest;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.exception.BookingClosedException;
import com.example.dentalbackend.exception.SessionFullException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicBookingServiceTest {

    @Test
    void rejectsMorningBookingsAtNoonEvenWhenSessionStatusIsOpen() {
        Instant noonInIndia = Instant.parse("2026-11-25T06:30:00Z");
        UUID sessionId = UUID.randomUUID();
        ClinicSession session = new ClinicSession();
        session.setType(SessionType.MORNING);
        session.setDate(LocalDate.of(2026, 11, 25));
        session.setBookingStatus(BookingStatus.OPEN);
        session.setBookingOpensAt(Instant.EPOCH);

        ClinicSessionRepository sessionRepository = mock(ClinicSessionRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        PublicBookingService service = new PublicBookingService(
                sessionRepository,
                bookingRepository,
                "clinic",
                Clock.fixed(noonInIndia, ZoneId.of("UTC"))
        );

        assertThrows(
                BookingClosedException.class,
                () -> service.createBooking(new CreatePublicBookingRequest(
                        sessionId.toString(),
                        "Patient",
                        "5550000000",
                        null
                ))
        );
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void reopenedMorningAcceptsBookingsUntilFullAndExpiresNextDay() {
        UUID sessionId = UUID.randomUUID();
        ClinicSession session = new ClinicSession();
        session.setId(sessionId);
        session.setType(SessionType.MORNING);
        session.setDate(LocalDate.of(2026, 11, 25));
        session.setBookingStatus(BookingStatus.OPEN);
        session.setBookingOpensAt(Instant.EPOCH);
        session.setMorningBookingOverride(true);
        session.setMaxAppointments(1);

        ClinicSessionRepository sessionRepository = mock(ClinicSessionRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreatePublicBookingRequest request = new CreatePublicBookingRequest(
                sessionId.toString(), "Patient", "5550000000", null
        );
        PublicBookingService service = new PublicBookingService(
                sessionRepository, bookingRepository, "clinic",
                Clock.fixed(Instant.parse("2026-11-25T06:30:00Z"), ZoneId.of("UTC"))
        );

        assertEquals(1, service.createBooking(request).tokenNo());
        when(bookingRepository.countActiveBySessionId(sessionId)).thenReturn(1L);
        assertThrows(SessionFullException.class, () -> service.createBooking(request));

        PublicBookingService nextDay = new PublicBookingService(
                sessionRepository, bookingRepository, "clinic",
                Clock.fixed(Instant.parse("2026-11-25T18:30:00Z"), ZoneId.of("UTC"))
        );
        assertThrows(BookingClosedException.class, () -> nextDay.createBooking(request));
    }
}
