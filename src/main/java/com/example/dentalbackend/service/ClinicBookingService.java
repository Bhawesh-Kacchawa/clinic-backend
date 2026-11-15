package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ClinicBookingService {

    private static final ZoneId CLINIC_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private final BookingRepository bookingRepository;

    public ClinicBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicBookingDto> getBookings(UUID clinicId, LocalDate requestedDate) {
        LocalDate date = requestedDate == null
                ? LocalDate.now(CLINIC_TIME_ZONE)
                : requestedDate;
        return bookingRepository.findClinicBookingsForDate(clinicId, date);
    }
}
