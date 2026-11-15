package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffQueueServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ClinicSessionRepository clinicSessionRepository;

    @InjectMocks
    private StaffQueueService staffQueueService;

    @Test
    void callNextCompletesCurrentBookingAndStartsNextBookedBooking() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking current = booking(sessionId, 1, BookingState.SERVING);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.SERVING))
                .thenReturn(List.of(current));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.BOOKED))
                .thenReturn(List.of(next));

        CallNextResponse response = staffQueueService.callNext(clinicId, sessionId);

        assertEquals(BookingState.DONE, current.getStatus());
        assertNotNull(current.getCompletedAt());
        assertEquals(BookingState.SERVING, next.getStatus());
        assertNotNull(next.getStartedAt());
        assertEquals(current.getId(), response.completedBooking().id());
        assertEquals(next.getId(), response.servingBooking().id());
        assertFalse(response.queueEmpty());
        verify(bookingRepository).findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.SERVING
        );
        verify(bookingRepository).findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.BOOKED
        );
    }

    @Test
    void callNextHandlesEmptyQueue() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                eq(sessionId),
                eq(clinicId),
                eq(BookingState.SERVING)
        )).thenReturn(List.of());
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                eq(sessionId),
                eq(clinicId),
                eq(BookingState.BOOKED)
        )).thenReturn(List.of());

        CallNextResponse response = staffQueueService.callNext(clinicId, sessionId);

        assertNull(response.completedBooking());
        assertNull(response.servingBooking());
        assertTrue(response.queueEmpty());
    }

    private Booking booking(UUID sessionId, int token, BookingState state) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setSession(session(sessionId));
        booking.setTokenNo(token);
        booking.setQueueOrder(token * 10);
        booking.setPatientName("Patient " + token);
        booking.setPhone("555000" + token);
        booking.setStatus(state);
        return booking;
    }

    private ClinicSession session(UUID id) {
        ClinicSession session = new ClinicSession();
        session.setId(id);
        return session;
    }
}
