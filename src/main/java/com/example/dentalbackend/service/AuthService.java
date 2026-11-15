package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.LoginRequest;
import com.example.dentalbackend.dto.LoginResponse;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.exception.InvalidCredentialsException;
import com.example.dentalbackend.repository.StaffUserRepository;
import com.example.dentalbackend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            StaffUserRepository staffUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.staffUserRepository = staffUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        StaffUser staffUser = staffUserRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(StaffUser::isActive)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.createToken(staffUser);
        return new LoginResponse(
                token,
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}
