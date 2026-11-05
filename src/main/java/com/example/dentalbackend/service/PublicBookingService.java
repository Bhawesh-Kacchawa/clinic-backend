package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CreatePublicBookingRequest;
import com.example.dentalbackend.dto.PublicBookingResponse;
import com.example.dentalbackend.dto.PublicBookingStatusResponse;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingSource;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.exception.BookingClosedException;
import com.example.dentalbackend.exception.DuplicateBookingException;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.exception.SessionFullException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PublicBookingService {

    private final ClinicSessionRepository clinicSessionRepository;
    private final BookingRepository bookingRepository;

    public PublicBookingService(
            ClinicSessionRepository clinicSessionRepository,
            BookingRepository bookingRepository
    ) {
        this.clinicSessionRepository = clinicSessionRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public PublicBookingResponse createBooking(CreatePublicBookingRequest request) {
        ClinicSession session = clinicSessionRepository.findByIdForUpdate(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));

        if (session.getBookingStatus() != BookingStatus.OPEN
                || Instant.now().isBefore(session.getBookingOpensAt())) {
            throw new BookingClosedException("Booking is not open for this session.");
        }

        if (bookingRepository.countActiveBySessionId(session.getId()) >= session.getMaxAppointments()) {
            throw new SessionFullException("This session is full.");
        }

        if (bookingRepository.existsBySession_IdAndPhoneAndStatusNot(
                session.getId(),
                request.phone(),
                BookingState.CANCELLED
        )) {
            throw new DuplicateBookingException("This phone number already has an active booking in this session.");
        }

        int tokenNo = bookingRepository.findMaxTokenNoBySessionId(session.getId()) + 1;
        Booking booking = new Booking();
        booking.setSession(session);
        booking.setTokenNo(tokenNo);
        booking.setQueueOrder(tokenNo * 10);
        booking.setPatientName(request.patientName());
        booking.setPhone(request.phone());
        booking.setReason(request.reason());
        booking.setStatus(BookingState.BOOKED);
        booking.setSource(BookingSource.ONLINE);

        Booking savedBooking = bookingRepository.save(booking);
        return new PublicBookingResponse(savedBooking.getId(), savedBooking.getTokenNo());
    }

    @Transactional(readOnly = true)
    public PublicBookingStatusResponse getBookingStatus(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        ClinicSession session = booking.getSession();
        long patientsAhead = bookingRepository.countPatientsAhead(session.getId(), booking.getQueueOrder());
        Integer currentServingToken = bookingRepository
                .findFirstBySession_IdAndStatusOrderByQueueOrderAsc(session.getId(), BookingState.SERVING)
                .map(Booking::getTokenNo)
                .orElse(null);
        LocalDateTime estimatedTime = LocalDateTime.of(session.getDate(), session.getStartTime())
                .plusMinutes(patientsAhead * session.getAvgMinutes());

        return new PublicBookingStatusResponse(
                booking.getId(),
                booking.getTokenNo(),
                booking.getStatus(),
                currentServingToken,
                patientsAhead,
                estimatedTime
        );
    }
}
