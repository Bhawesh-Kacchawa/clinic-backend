package com.example.dentalbackend.repository;

import com.example.dentalbackend.dto.PublicSessionDto;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClinicSessionRepository extends JpaRepository<ClinicSession, UUID> {

    boolean existsByClinic_IdAndDateAndType(UUID clinicId, LocalDate date, SessionType type);

    @Query("""
            select new com.example.dentalbackend.dto.PublicSessionDto(
                session.type,
                session.startTime,
                session.endTime,
                session.maxAppointments,
                (select count(booking)
                 from Booking booking
                 where booking.session = session
                   and booking.status <> com.example.dentalbackend.entity.BookingState.CANCELLED),
                session.bookingStatus,
                session.bookingOpensAt
            )
            from ClinicSession session
            where session.clinic.id = :clinicId
              and session.date = :date
            order by session.startTime
            """)
    List<PublicSessionDto> findPublicSessions(
            @Param("clinicId") UUID clinicId,
            @Param("date") LocalDate date
    );
}
