package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.CreatePublicBookingRequest;
import com.example.dentalbackend.dto.PublicBookingResponse;
import com.example.dentalbackend.dto.PublicBookingStatusResponse;
import com.example.dentalbackend.service.PublicBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/bookings")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    public PublicBookingController(PublicBookingService publicBookingService) {
        this.publicBookingService = publicBookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicBookingResponse createBooking(@Valid @RequestBody CreatePublicBookingRequest request) {
        return publicBookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    public PublicBookingStatusResponse getBookingStatus(@PathVariable UUID id) {
        return publicBookingService.getBookingStatus(id);
    }
}
