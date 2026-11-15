package com.example.dentalbackend.repository;

import com.example.dentalbackend.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {

    Optional<StaffUser> findByEmailIgnoreCase(String email);

    Optional<StaffUser> findByIdAndClinic_IdAndActiveTrue(UUID id, UUID clinicId);
}
