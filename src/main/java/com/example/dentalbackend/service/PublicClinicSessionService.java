package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.PublicSessionDto;
import com.example.dentalbackend.dto.PublicSessionsResponse;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class PublicClinicSessionService {

    private static final ZoneId CLINIC_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private final ClinicRepository clinicRepository;
    private final ClinicSessionRepository clinicSessionRepository;

    public PublicClinicSessionService(
            ClinicRepository clinicRepository,
            ClinicSessionRepository clinicSessionRepository
    ) {
        this.clinicRepository = clinicRepository;
        this.clinicSessionRepository = clinicSessionRepository;
    }

    @Transactional(readOnly = true)
    public PublicSessionsResponse getTodaysSessions(String clinicSlug) {
        Instant serverTime = Instant.now();
        LocalDate today = serverTime.atZone(CLINIC_TIME_ZONE).toLocalDate();

        Clinic clinic = clinicRepository.findBySlug(clinicSlug)
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        List<PublicSessionDto> sessions = clinicSessionRepository.findPublicSessions(clinic.getId(), today);

        return new PublicSessionsResponse(serverTime, sessions);
    }
}
