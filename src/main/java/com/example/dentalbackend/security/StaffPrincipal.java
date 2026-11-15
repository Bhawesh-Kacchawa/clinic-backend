package com.example.dentalbackend.security;

import com.example.dentalbackend.entity.StaffRole;

import java.util.UUID;

public record StaffPrincipal(UUID staffId, UUID clinicId, StaffRole role) {
}
