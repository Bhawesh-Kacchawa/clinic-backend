package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.CreateClinicSessionRequest;
import com.example.dentalbackend.dto.StaffClinicSessionResponse;
import com.example.dentalbackend.dto.UpdateClinicSessionRequest;
import com.example.dentalbackend.dto.UpdateClinicSessionStatusRequest;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.StaffClinicSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/staff/sessions")
public class StaffClinicSessionController {

    private final StaffClinicSessionService clinicSessionService;

    public StaffClinicSessionController(StaffClinicSessionService clinicSessionService) {
        this.clinicSessionService = clinicSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffClinicSessionResponse create(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody CreateClinicSessionRequest request
    ) {
        return clinicSessionService.create(principal, request);
    }

    @PatchMapping
    public StaffClinicSessionResponse update(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody UpdateClinicSessionRequest request
    ) {
        return clinicSessionService.update(principal.clinicId(), request);
    }

    @PatchMapping("/{id}/status")
    public StaffClinicSessionResponse updateStatus(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicSessionStatusRequest request
    ) {
        return clinicSessionService.updateStatus(principal.clinicId(), id, request.action());
    }
}
