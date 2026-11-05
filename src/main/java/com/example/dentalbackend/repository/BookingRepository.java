package com.example.dentalbackend.repository;

import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
            select count(booking)
            from Booking booking
            where booking.session.id = :sessionId
              and booking.status <> com.example.dentalbackend.entity.BookingState.CANCELLED
            """)
    long countActiveBySessionId(@Param("sessionId") UUID sessionId);

    boolean existsBySession_IdAndPhoneAndStatusNot(
            UUID sessionId,
            String phone,
            BookingState excludedStatus
    );

    @Query("select coalesce(max(booking.tokenNo), 0) from Booking booking where booking.session.id = :sessionId")
    int findMaxTokenNoBySessionId(@Param("sessionId") UUID sessionId);

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
}
