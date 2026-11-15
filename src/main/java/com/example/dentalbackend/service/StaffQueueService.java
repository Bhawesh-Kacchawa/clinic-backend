package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.dto.StaffQueueBookingResponse;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.exception.QueueStateConflictException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StaffQueueService {

    private final BookingRepository bookingRepository;
    private final ClinicSessionRepository clinicSessionRepository;

    public StaffQueueService(
            BookingRepository bookingRepository,
            ClinicSessionRepository clinicSessionRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.clinicSessionRepository = clinicSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffQueueBookingResponse> getQueue(UUID clinicId, UUID sessionId) {
        clinicSessionRepository.findByIdAndClinic_Id(sessionId, clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
        return bookingRepository.findBySession_IdAndSession_Clinic_IdOrderByQueueOrderAsc(sessionId, clinicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CallNextResponse callNext(UUID clinicId, UUID sessionId) {
        lockOwnedSession(sessionId, clinicId);
        Instant now = Instant.now();

        Booking currentServing = bookingRepository
                .findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.SERVING)
                .stream()
                .findFirst()
                .orElse(null);
        if (currentServing != null) {
            currentServing.setStatus(BookingState.DONE);
            currentServing.setCompletedAt(now);
            bookingRepository.save(currentServing);
        }

        Booking nextBooking = bookingRepository
                .findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.BOOKED)
                .stream()
                .findFirst()
                .orElse(null);
        if (nextBooking != null) {
            nextBooking.setStatus(BookingState.SERVING);
            nextBooking.setStartedAt(now);
            bookingRepository.save(nextBooking);
        }

        return new CallNextResponse(
                currentServing == null ? null : toResponse(currentServing),
                nextBooking == null ? null : toResponse(nextBooking),
                nextBooking == null
        );
    }

    @Transactional
    public StaffQueueBookingResponse callManually(UUID clinicId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndSession_Clinic_Id(bookingId, clinicId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        UUID sessionId = booking.getSession().getId();
        lockOwnedSession(sessionId, clinicId);
        booking = bookingRepository.findByIdAndClinicIdForUpdate(bookingId, clinicId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        if (booking.getStatus() != BookingState.BOOKED) {
            throw new QueueStateConflictException("Only a booked token can be called manually.");
        }

        booking.setStatus(BookingState.CALLED);
        booking.setCalledAt(Instant.now());
        return toResponse(bookingRepository.save(booking));
    }

    private void lockOwnedSession(UUID sessionId, UUID clinicId) {
        clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
    }

    private StaffQueueBookingResponse toResponse(Booking booking) {
        return new StaffQueueBookingResponse(
                booking.getId(),
                booking.getTokenNo(),
                booking.getQueueOrder(),
                booking.getPatientName(),
                booking.getPhone(),
                booking.getReason(),
                booking.getStatus(),
                booking.getCalledAt(),
                booking.getStartedAt(),
                booking.getCompletedAt()
        );
    }
}
