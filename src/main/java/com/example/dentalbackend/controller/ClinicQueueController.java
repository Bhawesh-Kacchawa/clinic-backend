package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.ClinicCallNextResponse;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.StaffQueueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinics/{slug}/sessions/{type}")
public class ClinicQueueController {

    private final StaffQueueService staffQueueService;

    public ClinicQueueController(StaffQueueService staffQueueService) {
        this.staffQueueService = staffQueueService;
    }

    @PostMapping("/call-next")
    public ClinicCallNextResponse callNext(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable String slug,
            @PathVariable SessionType type
    ) {
        return staffQueueService.callNextForToday(
                principal.staffId(),
                principal.clinicId(),
                slug,
                type
        );
    }
}
