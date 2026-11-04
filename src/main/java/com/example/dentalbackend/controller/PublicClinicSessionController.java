package com.example.dentalbackend.controller;

import com.example.dentalbackend.dto.PublicSessionsResponse;
import com.example.dentalbackend.service.PublicClinicSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/clinics/{slug}/sessions")
public class PublicClinicSessionController {

    private final PublicClinicSessionService publicClinicSessionService;

    public PublicClinicSessionController(PublicClinicSessionService publicClinicSessionService) {
        this.publicClinicSessionService = publicClinicSessionService;
    }

    @GetMapping
    public PublicSessionsResponse getTodaysSessions(@PathVariable("slug") String slug) {
        return publicClinicSessionService.getTodaysSessions(slug);
    }
}
