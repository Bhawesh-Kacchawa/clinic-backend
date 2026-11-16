package com.example.dentalbackend.repository;

import com.example.dentalbackend.dto.StaffAuditLogResponse;
import com.example.dentalbackend.entity.AuditLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select new com.example.dentalbackend.dto.StaffAuditLogResponse(
                log.id,
                log.actor.id,
                log.entityId,
                log.tokenNo,
                log.action,
                log.beforeStatus,
                log.afterStatus,
                log.createdAt
            )
            from AuditLog log
            where log.clinic.id = :clinicId
              and log.entityType = 'BOOKING'
            order by log.auditOrder desc
            """)
    List<StaffAuditLogResponse> findStaffQueueAuditLog(@Param("clinicId") UUID clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select log from AuditLog log
            where log.clinic.id = :clinicId
              and log.entityType = 'BOOKING'
              and log.entityId = :bookingId
            order by log.auditOrder desc
            """)
    List<AuditLog> findBookingAuditLogForUpdate(
            @Param("clinicId") UUID clinicId,
            @Param("bookingId") UUID bookingId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select log from AuditLog log
            where log.clinic.id = :clinicId
              and log.operationId = :operationId
            order by log.auditOrder
            """)
    List<AuditLog> findOperationAuditLogForUpdate(
            @Param("clinicId") UUID clinicId,
            @Param("operationId") UUID operationId
    );
}
