package com.example.dentalbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private StaffUser actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "audit_order", insertable = false, updatable = false)
    private Long auditOrder;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "token_no")
    private Integer tokenNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 20)
    private BookingState beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", length = 20)
    private BookingState afterStatus;

    @Column(name = "queue_order_before")
    private Integer queueOrderBefore;

    @Column(name = "queue_order_after")
    private Integer queueOrderAfter;

    @Column(name = "skip_count_before")
    private Integer skipCountBefore;

    @Column(name = "skip_count_after")
    private Integer skipCountAfter;

    @Column(name = "called_at_before")
    private Instant calledAtBefore;

    @Column(name = "called_at_after")
    private Instant calledAtAfter;

    @Column(name = "started_at_before")
    private Instant startedAtBefore;

    @Column(name = "started_at_after")
    private Instant startedAtAfter;

    @Column(name = "completed_at_before")
    private Instant completedAtBefore;

    @Column(name = "completed_at_after")
    private Instant completedAtAfter;

    @Column(nullable = false)
    private boolean undone;

    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }
}
