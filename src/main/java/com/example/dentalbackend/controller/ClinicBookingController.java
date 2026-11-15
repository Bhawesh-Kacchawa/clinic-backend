package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.ClinicBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff/bookings")
public class ClinicBookingController {

    private final ClinicBookingService clinicBookingService;

    public ClinicBookingController(ClinicBookingService clinicBookingService) {
        this.clinicBookingService = clinicBookingService;
    }

    @GetMapping
    public List<ClinicBookingDto> getBookings(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return clinicBookingService.getBookings(staffPrincipal.clinicId(), date);
    }
}
