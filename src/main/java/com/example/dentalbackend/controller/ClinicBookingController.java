package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.service.ClinicBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clinics/{clinicSlug}/bookings")
public class ClinicBookingController {

    private final ClinicBookingService clinicBookingService;

    public ClinicBookingController(ClinicBookingService clinicBookingService) {
        this.clinicBookingService = clinicBookingService;
    }

    @GetMapping
    public List<ClinicBookingDto> getBookings(
            @PathVariable String clinicSlug,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return clinicBookingService.getBookings(clinicSlug, date);
    }
}
