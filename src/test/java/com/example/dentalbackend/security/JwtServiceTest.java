package com.example.dentalbackend.security;

import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.StaffRole;
import com.example.dentalbackend.entity.StaffUser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void tokenContainsStaffClinicAndRoleClaims() {
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-with-more-than-32-bytes-for-hmac".getBytes(StandardCharsets.UTF_8)
        );
        JwtService jwtService = new JwtService(secret, 3600);

        UUID staffId = UUID.randomUUID();
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic();
        clinic.setId(clinicId);
        StaffUser staffUser = new StaffUser();
        staffUser.setId(staffId);
        staffUser.setClinic(clinic);
        staffUser.setEmail("doctor@example.com");
        staffUser.setRole(StaffRole.DOCTOR);

        StaffPrincipal principal = jwtService.parseToken(jwtService.createToken(staffUser));

        assertEquals(staffId, principal.staffId());
        assertEquals(clinicId, principal.clinicId());
        assertEquals(StaffRole.DOCTOR, principal.role());
    }
}
