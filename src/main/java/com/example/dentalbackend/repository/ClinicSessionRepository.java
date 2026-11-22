package com.example.dentalbackend.repository;

import com.example.dentalbackend.dto.PublicSessionDto;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicSessionRepository extends JpaRepository<ClinicSession, UUID> {

    Optional<ClinicSession> findByClinic_IdAndDateAndType(UUID clinicId, LocalDate date, SessionType type);

    Optional<ClinicSession> findByIdAndClinic_Id(UUID id, UUID clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from ClinicSession session
            where session.id = :id
              and session.clinic.id = :clinicId
            """)
    Optional<ClinicSession> findByIdAndClinic_IdForUpdate(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ClinicSession session where session.id = :id")
    Optional<ClinicSession> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from ClinicSession session
            where session.clinic.slug = :clinicSlug
              and session.date = :date
              and session.type = :type
            """)
    Optional<ClinicSession> findByClinicSlugAndDateAndTypeForUpdate(
            @Param("clinicSlug") String clinicSlug,
            @Param("date") LocalDate date,
            @Param("type") SessionType type
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from ClinicSession session
            where session.clinic.id = :clinicId
              and session.clinic.slug = :clinicSlug
              and session.date = :date
              and session.type = :type
            """)
    Optional<ClinicSession> findByClinicIdAndSlugAndDateAndTypeForUpdate(
            @Param("clinicId") UUID clinicId,
            @Param("clinicSlug") String clinicSlug,
            @Param("date") LocalDate date,
            @Param("type") SessionType type
    );

    @Query("""
            select session from ClinicSession session
            where session.clinic.id = :clinicId
              and session.clinic.slug = :clinicSlug
              and session.date = :date
              and session.type = :type
            """)
    Optional<ClinicSession> findByClinicIdAndSlugAndDateAndType(
            @Param("clinicId") UUID clinicId,
            @Param("clinicSlug") String clinicSlug,
            @Param("date") LocalDate date,
            @Param("type") SessionType type
    );

    @Query("""
            select new com.example.dentalbackend.dto.PublicSessionDto(
                session.id,
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
