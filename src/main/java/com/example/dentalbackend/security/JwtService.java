package com.example.dentalbackend.security;

import com.example.dentalbackend.entity.StaffRole;
import com.example.dentalbackend.entity.StaffUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero.");
        }
    }

    public String createToken(StaffUser staffUser) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(staffUser.getEmail())
                .claim("staffId", staffUser.getId().toString())
                .claim("clinicId", staffUser.getClinic().getId().toString())
                .claim("role", staffUser.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public StaffPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new StaffPrincipal(
                UUID.fromString(requiredStringClaim(claims, "staffId")),
                UUID.fromString(requiredStringClaim(claims, "clinicId")),
                StaffRole.valueOf(requiredStringClaim(claims, "role"))
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String requiredStringClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT is missing required claim: " + name);
        }
        return value;
    }
}
