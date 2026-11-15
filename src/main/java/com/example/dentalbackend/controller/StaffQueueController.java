package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.CallNextRequest;
import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.dto.StaffQueueBookingResponse;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.StaffQueueService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
public class StaffQueueController {

    private final StaffQueueService staffQueueService;

    public StaffQueueController(StaffQueueService staffQueueService) {
        this.staffQueueService = staffQueueService;
    }

    @GetMapping("/sessions/{id}/queue")
    public List<StaffQueueBookingResponse> getQueue(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID id
    ) {
        return staffQueueService.getQueue(principal.clinicId(), id);
    }

    @PostMapping("/queue/call-next")
    public CallNextResponse callNext(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody CallNextRequest request
    ) {
        return staffQueueService.callNext(principal.clinicId(), request.sessionId());
    }

    @PostMapping("/queue/call/{bookingId}")
    public StaffQueueBookingResponse callManually(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return staffQueueService.callManually(principal.clinicId(), bookingId);
    }
}
