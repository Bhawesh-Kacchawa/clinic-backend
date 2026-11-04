package com.example.dentalbackend.repository;

import com.example.dentalbackend.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findBySlug(String slug);
}
