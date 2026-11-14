package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ClinicBookingService {

    private static final ZoneId CLINIC_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private final BookingRepository bookingRepository;
    private final ClinicRepository clinicRepository;

    public ClinicBookingService(BookingRepository bookingRepository, ClinicRepository clinicRepository) {
        this.bookingRepository = bookingRepository;
        this.clinicRepository = clinicRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicBookingDto> getBookings(String clinicSlug, LocalDate requestedDate) {
        LocalDate date = requestedDate == null
                ? LocalDate.now(CLINIC_TIME_ZONE)
                : requestedDate;
        clinicRepository.findBySlug(clinicSlug)
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        return bookingRepository.findClinicBookingsForDate(clinicSlug, date);
    }
}
