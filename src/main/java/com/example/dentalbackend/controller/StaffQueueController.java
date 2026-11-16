package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.CallNextRequest;
import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.dto.CreateWalkInBookingRequest;
import com.example.dentalbackend.dto.QueueSkipResponse;
import com.example.dentalbackend.dto.StaffAuditLogResponse;
import com.example.dentalbackend.dto.StaffQueueBookingResponse;
import com.example.dentalbackend.dto.UndoQueueActionRequest;
import com.example.dentalbackend.security.StaffPrincipal;
import com.example.dentalbackend.service.StaffQueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
        return staffQueueService.callNext(principal.staffId(), principal.clinicId(), request.sessionId());
    }

    @PostMapping("/queue/call/{bookingId}")
    public StaffQueueBookingResponse callManually(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return staffQueueService.callManually(principal.staffId(), principal.clinicId(), bookingId);
    }

    @PostMapping("/queue/{bookingId}/skip")
    public QueueSkipResponse skip(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return staffQueueService.skip(principal.staffId(), principal.clinicId(), bookingId);
    }

    @PostMapping("/queue/{bookingId}/requeue")
    public StaffQueueBookingResponse requeue(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return staffQueueService.requeue(principal.staffId(), principal.clinicId(), bookingId);
    }

    @PostMapping("/queue/{bookingId}/no-show")
    public StaffQueueBookingResponse markNoShow(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return staffQueueService.markNoShow(principal.staffId(), principal.clinicId(), bookingId);
    }

    @PostMapping("/bookings/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffQueueBookingResponse createWalkIn(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody CreateWalkInBookingRequest request
    ) {
        return staffQueueService.createWalkIn(principal.staffId(), principal.clinicId(), request);
    }

    @PostMapping("/queue/undo")
    public StaffQueueBookingResponse undo(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody UndoQueueActionRequest request
    ) {
        return staffQueueService.undo(principal.staffId(), principal.clinicId(), request.bookingId());
    }

    @GetMapping("/audit-log")
    public List<StaffAuditLogResponse> getAuditLog(
            @AuthenticationPrincipal StaffPrincipal principal
    ) {
        return staffQueueService.getAuditLog(principal.clinicId());
    }
}
