package com.example.dentalbackend.repository;

import com.example.dentalbackend.dto.ClinicBookingDto;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
            select count(booking)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.status <> com.example.dentalbackend.entity.BookingState.CANCELLED
            """)
    long countActiveBySessionId(@Param("sessionId") UUID sessionId);

    @Query("""
            select count(booking)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.session.clinic.id = :clinicId
              and booking.status <> com.example.dentalbackend.entity.BookingState.CANCELLED
            """)
    long countActiveBySessionIdAndClinicId(
            @Param("sessionId") UUID sessionId,
            @Param("clinicId") UUID clinicId
    );

    boolean existsBySession_IdAndPhoneAndStatusNot(
            UUID sessionId,
            String phone,
            BookingState excludedStatus
    );

    @Query("select coalesce(max(booking.tokenNo), 0) from Booking booking where booking.session.id = :sessionId")
    int findMaxTokenNoBySessionId(@Param("sessionId") UUID sessionId);

    @Query("""
            select coalesce(max(booking.tokenNo), 0)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.session.clinic.id = :clinicId
            """)
    int findMaxTokenNoBySessionIdAndClinicId(
            @Param("sessionId") UUID sessionId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            select coalesce(max(booking.queueOrder), 0)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.session.clinic.id = :clinicId
            """)
    int findMaxQueueOrderBySessionIdAndClinicId(
            @Param("sessionId") UUID sessionId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            select count(booking)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.queueOrder < :queueOrder
              and booking.status in (
                  com.example.dentalbackend.entity.BookingState.BOOKED,
                  com.example.dentalbackend.entity.BookingState.CALLED
              )
            """)
    long countPatientsAhead(
            @Param("sessionId") UUID sessionId,
            @Param("queueOrder") int queueOrder
    );

    Optional<Booking> findFirstBySession_IdAndStatusOrderByQueueOrderAsc(
            UUID sessionId,
            BookingState status
    );

    List<Booking> findBySession_IdAndSession_Clinic_IdOrderByQueueOrderAsc(
            UUID sessionId,
            UUID clinicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from Booking booking
            where booking.session.id = :sessionId
              and booking.session.clinic.id = :clinicId
            order by booking.queueOrder
            """)
    List<Booking> findQueueBySessionIdAndClinicIdForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("clinicId") UUID clinicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from Booking booking
            where booking.session.id = :sessionId
              and booking.session.clinic.id = :clinicId
              and booking.status = :status
            order by booking.queueOrder
            """)
    List<Booking> findBySessionIdAndStatusForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("clinicId") UUID clinicId,
            @Param("status") BookingState status
    );

    Optional<Booking> findByIdAndSession_Clinic_Id(UUID id, UUID clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from Booking booking
            where booking.id = :bookingId
              and booking.session.clinic.id = :clinicId
            """)
    Optional<Booking> findByIdAndClinicIdForUpdate(
            @Param("bookingId") UUID bookingId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            select new com.example.dentalbackend.dto.ClinicBookingDto(
                booking.id,
                booking.tokenNo,
                booking.patientName,
                booking.phone,
                booking.status,
                session.type,
                session.date,
                session.startTime,
                session.endTime
            )
            from Booking booking
            join booking.session session
            where session.clinic.id = :clinicId
              and session.date = :date
            order by session.startTime, booking.queueOrder
            """)
    List<ClinicBookingDto> findClinicBookingsForDate(
            @Param("clinicId") UUID clinicId,
            @Param("date") LocalDate date
    );
}
