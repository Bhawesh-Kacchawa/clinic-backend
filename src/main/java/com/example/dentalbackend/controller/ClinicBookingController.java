package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.dto.StaffQueueBookingResponse;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.ClinicBookingService;
import com.example.dentalbackend.service.StaffQueueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ClinicBookingController {

    private final ClinicBookingService clinicBookingService;
    private final StaffQueueService staffQueueService;

    public ClinicBookingController(
            ClinicBookingService clinicBookingService,
            StaffQueueService staffQueueService
    ) {
        this.clinicBookingService = clinicBookingService;
        this.staffQueueService = staffQueueService;
    }

    @GetMapping("/staff/bookings")
    public List<ClinicBookingDto> getBookings(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return clinicBookingService.getBookings(staffPrincipal.clinicId(), date);
    }

    @PatchMapping("/clinics/{slug}/bookings/{id}/status")
    public StaffQueueBookingResponse serveBooking(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @PathVariable String slug,
            @PathVariable UUID id
    ) {
        return staffQueueService.serveManually(
                staffPrincipal.staffId(),
                staffPrincipal.clinicId(),
                slug,
                id
        );
    }
}
