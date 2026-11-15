package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CreateClinicSessionRequest;
import com.example.dentalbackend.dto.StaffClinicSessionResponse;
import com.example.dentalbackend.dto.UpdateClinicSessionRequest;
import com.example.dentalbackend.dto.UpdateClinicSessionStatusRequest.BookingAction;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.StaffRole;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.exception.DuplicateClinicSessionException;
import com.example.dentalbackend.exception.InvalidStaffRequestException;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import com.example.dentalbackend.security.StaffPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class StaffClinicSessionService {

    private final ClinicSessionRepository clinicSessionRepository;
    private final ClinicRepository clinicRepository;
    private final StaffUserRepository staffUserRepository;
    private final BookingRepository bookingRepository;

    public StaffClinicSessionService(
            ClinicSessionRepository clinicSessionRepository,
            ClinicRepository clinicRepository,
            StaffUserRepository staffUserRepository,
            BookingRepository bookingRepository
    ) {
        this.clinicSessionRepository = clinicSessionRepository;
        this.clinicRepository = clinicRepository;
        this.staffUserRepository = staffUserRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public StaffClinicSessionResponse create(
            StaffPrincipal principal,
            CreateClinicSessionRequest request
    ) {
        if (principal.role() != StaffRole.DOCTOR) {
            throw new InvalidStaffRequestException("Only a doctor can create a session.");
        }
        validateTimes(request.startTime(), request.endTime());

        Clinic clinic = clinicRepository.findById(principal.clinicId())
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        StaffUser doctor = staffUserRepository
                .findByIdAndClinic_IdAndActiveTrue(principal.staffId(), principal.clinicId())
                .filter(user -> user.getRole() == StaffRole.DOCTOR)
                .orElseThrow(() -> new NotFoundException("Active doctor not found in this clinic."));

        if (clinicSessionRepository.findByClinic_IdAndDateAndType(
                principal.clinicId(),
                request.date(),
                request.type()
        ).isPresent()) {
            throw new DuplicateClinicSessionException();
        }

        ClinicSession session = new ClinicSession();
        session.setClinic(clinic);
        session.setDoctor(doctor);
        session.setType(request.type());
        session.setDate(request.date());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setMaxAppointments(request.maxAppointments());
        session.setAvgMinutes(request.avgMinutes());
        session.setBookingOpensAt(request.bookingOpensAt());
        session.setBookingStatus(
                Instant.now().isBefore(request.bookingOpensAt())
                        ? BookingStatus.SCHEDULED
                        : BookingStatus.OPEN
        );
        return toResponse(clinicSessionRepository.save(session));
    }

    @Transactional
    public StaffClinicSessionResponse update(
            UUID clinicId,
            UpdateClinicSessionRequest request
    ) {
        if (request.startTime() == null
                && request.endTime() == null
                && request.maxAppointments() == null
                && request.avgMinutes() == null
                && request.bookingOpensAt() == null) {
            throw new InvalidStaffRequestException("At least one session field must be provided.");
        }

        ClinicSession session = findOwnedSession(request.id(), clinicId);
        var startTime = Objects.requireNonNullElse(request.startTime(), session.getStartTime());
        var endTime = Objects.requireNonNullElse(request.endTime(), session.getEndTime());
        validateTimes(startTime, endTime);

        if (request.maxAppointments() != null
                && request.maxAppointments()
                < bookingRepository.countActiveBySessionIdAndClinicId(session.getId(), clinicId)) {
            throw new InvalidStaffRequestException(
                    "maxAppointments cannot be less than the number of active bookings."
            );
        }

        session.setStartTime(startTime);
        session.setEndTime(endTime);
        if (request.maxAppointments() != null) {
            session.setMaxAppointments(request.maxAppointments());
        }
        if (request.avgMinutes() != null) {
            session.setAvgMinutes(request.avgMinutes());
        }
        if (request.bookingOpensAt() != null) {
            session.setBookingOpensAt(request.bookingOpensAt());
            if (session.getBookingStatus() == BookingStatus.SCHEDULED
                    && !Instant.now().isBefore(request.bookingOpensAt())) {
                session.setBookingStatus(BookingStatus.OPEN);
            }
        }
        return toResponse(clinicSessionRepository.save(session));
    }

    @Transactional
    public StaffClinicSessionResponse updateStatus(
            UUID clinicId,
            UUID sessionId,
            BookingAction action
    ) {
        ClinicSession session = findOwnedSession(sessionId, clinicId);
        session.setBookingStatus(switch (action) {
            case OPEN -> BookingStatus.OPEN;
            case PAUSE -> BookingStatus.PAUSED;
            case CLOSE -> BookingStatus.CLOSED;
        });
        return toResponse(clinicSessionRepository.save(session));
    }

    private ClinicSession findOwnedSession(UUID sessionId, UUID clinicId) {
        return clinicSessionRepository.findByIdAndClinic_Id(sessionId, clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
    }

    private StaffClinicSessionResponse toResponse(ClinicSession session) {
        return new StaffClinicSessionResponse(
                session.getId(),
                session.getDoctor().getId(),
                session.getType(),
                session.getDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getMaxAppointments(),
                session.getAvgMinutes(),
                session.getBookingOpensAt(),
                session.getBookingStatus()
        );
    }

    private void validateTimes(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidStaffRequestException("startTime must be before endTime.");
        }
    }
}
