package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.dto.CreateWalkInBookingRequest;
import com.example.dentalbackend.dto.QueueSkipResponse;
import com.example.dentalbackend.dto.StaffAuditLogResponse;
import com.example.dentalbackend.dto.StaffQueueBookingResponse;
import com.example.dentalbackend.entity.AuditLog;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingSource;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.exception.NotFoundException;
import com.example.dentalbackend.exception.QueueStateConflictException;
import com.example.dentalbackend.exception.SessionFullException;
import com.example.dentalbackend.repository.AuditLogRepository;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StaffQueueService {

    private final BookingRepository bookingRepository;
    private final ClinicSessionRepository clinicSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final StaffUserRepository staffUserRepository;

    public StaffQueueService(
            BookingRepository bookingRepository,
            ClinicSessionRepository clinicSessionRepository,
            AuditLogRepository auditLogRepository,
            StaffUserRepository staffUserRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.clinicSessionRepository = clinicSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.staffUserRepository = staffUserRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffQueueBookingResponse> getQueue(UUID clinicId, UUID sessionId) {
        clinicSessionRepository.findByIdAndClinic_Id(sessionId, clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
        return bookingRepository.findBySession_IdAndSession_Clinic_IdOrderByQueueOrderAsc(sessionId, clinicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CallNextResponse callNext(UUID staffId, UUID clinicId, UUID sessionId) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        ClinicSession session = lockOwnedSession(sessionId, clinicId);
        Instant now = Instant.now();

        Booking currentServing = bookingRepository
                .findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.SERVING)
                .stream()
                .findFirst()
                .orElse(null);
        if (currentServing != null) {
            BookingSnapshot before = snapshot(currentServing);
            currentServing.setStatus(BookingState.DONE);
            currentServing.setCompletedAt(now);
            bookingRepository.save(currentServing);
            recordAudit(currentServing, session, actor, operationId, "CALL_NEXT_COMPLETE", before);
        }

        Booking nextBooking = bookingRepository
                .findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.BOOKED)
                .stream()
                .findFirst()
                .orElse(null);
        if (nextBooking != null) {
            BookingSnapshot before = snapshot(nextBooking);
            nextBooking.setStatus(BookingState.SERVING);
            nextBooking.setStartedAt(now);
            bookingRepository.save(nextBooking);
            recordAudit(nextBooking, session, actor, operationId, "CALL_NEXT_SERVE", before);
        }

        return new CallNextResponse(
                currentServing == null ? null : toResponse(currentServing),
                nextBooking == null ? null : toResponse(nextBooking),
                nextBooking == null
        );
    }

    @Transactional
    public StaffQueueBookingResponse callManually(UUID staffId, UUID clinicId, UUID bookingId) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        Booking booking = findLockedBooking(clinicId, bookingId);
        if (booking.getStatus() != BookingState.BOOKED) {
            throw new QueueStateConflictException("Only a booked token can be called manually.");
        }

        BookingSnapshot before = snapshot(booking);
        booking.setStatus(BookingState.CALLED);
        booking.setCalledAt(Instant.now());
        bookingRepository.save(booking);
        recordAudit(booking, booking.getSession(), actor, operationId, "CALL", before);
        return toResponse(booking);
    }

    @Transactional
    public QueueSkipResponse skip(UUID staffId, UUID clinicId, UUID bookingId) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        Booking booking = findLockedBooking(clinicId, bookingId);
        if (booking.getStatus() != BookingState.BOOKED
                && booking.getStatus() != BookingState.CALLED
                && booking.getStatus() != BookingState.SERVING) {
            throw new QueueStateConflictException("Only a booked, called, or serving token can be skipped.");
        }

        BookingSnapshot before = snapshot(booking);
        booking.setStatus(BookingState.SKIPPED);
        booking.setSkipCount(booking.getSkipCount() + 1);
        bookingRepository.save(booking);
        recordAudit(booking, booking.getSession(), actor, operationId, "SKIP", before);

        List<Booking> servingBookings = bookingRepository.findBySessionIdAndStatusForUpdate(
                booking.getSession().getId(),
                clinicId,
                BookingState.SERVING
        );
        Booking serving = servingBookings.stream().findFirst().orElse(null);
        if (serving == null) {
            serving = callNextBooked(booking.getSession(), clinicId, actor, operationId);
        }
        return new QueueSkipResponse(toResponse(booking), serving == null ? null : toResponse(serving));
    }

    @Transactional
    public StaffQueueBookingResponse requeue(UUID staffId, UUID clinicId, UUID bookingId) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        Booking booking = findLockedBooking(clinicId, bookingId);
        if (booking.getStatus() != BookingState.SKIPPED && booking.getStatus() != BookingState.NO_SHOW) {
            throw new QueueStateConflictException("Only a skipped or no-show token can be requeued.");
        }

        UUID sessionId = booking.getSession().getId();
        List<Booking> queue = bookingRepository.findQueueBySessionIdAndClinicIdForUpdate(sessionId, clinicId);
        Map<UUID, BookingSnapshot> beforeSnapshots = new HashMap<>();
        queue.forEach(item -> beforeSnapshots.put(item.getId(), snapshot(item)));
        Booking currentServing = queue.stream()
                .filter(item -> !item.getId().equals(bookingId))
                .filter(item -> item.getStatus() == BookingState.SERVING)
                .findFirst()
                .orElse(null);

        booking.setStatus(BookingState.BOOKED);
        if (currentServing == null) {
            int maxQueueOrder = queue.stream()
                    .filter(item -> !item.getId().equals(bookingId))
                    .mapToInt(Booking::getQueueOrder)
                    .max()
                    .orElse(0);
            booking.setQueueOrder(nextQueueOrder(maxQueueOrder));
        } else {
            List<Booking> orderedWithoutRequeuedBooking = new ArrayList<>(queue.stream()
                    .filter(item -> !item.getId().equals(bookingId))
                    .toList());
            int servingIndex = indexOfBooking(orderedWithoutRequeuedBooking, currentServing.getId());
            Booking nextInQueue = servingIndex + 1 < orderedWithoutRequeuedBooking.size()
                    ? orderedWithoutRequeuedBooking.get(servingIndex + 1)
                    : null;

            if (nextInQueue != null
                    && (long) nextInQueue.getQueueOrder() - currentServing.getQueueOrder() > 1) {
                booking.setQueueOrder(
                        currentServing.getQueueOrder()
                                + (nextInQueue.getQueueOrder() - currentServing.getQueueOrder()) / 2
                );
            } else {
                orderedWithoutRequeuedBooking.add(servingIndex + 1, booking);
                if (orderedWithoutRequeuedBooking.size() > Integer.MAX_VALUE / 10) {
                    throw new QueueStateConflictException("Queue positions cannot be rebalanced.");
                }
                for (int index = 0; index < orderedWithoutRequeuedBooking.size(); index++) {
                    orderedWithoutRequeuedBooking.get(index).setQueueOrder((index + 1) * 10);
                }
            }
        }

        for (Booking changedBooking : queue) {
            BookingSnapshot before = beforeSnapshots.get(changedBooking.getId());
            if (before.queueOrder() != changedBooking.getQueueOrder()
                    || before.status() != changedBooking.getStatus()) {
                bookingRepository.save(changedBooking);
                recordAudit(
                        changedBooking,
                        changedBooking.getSession(),
                        actor,
                        operationId,
                        changedBooking.getId().equals(bookingId) ? "REQUEUE" : "REQUEUE_ORDER",
                        before
                );
            }
        }
        return toResponse(booking);
    }

    @Transactional
    public StaffQueueBookingResponse markNoShow(UUID staffId, UUID clinicId, UUID bookingId) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        Booking booking = findLockedBooking(clinicId, bookingId);
        if (booking.getStatus() != BookingState.BOOKED && booking.getStatus() != BookingState.CALLED) {
            throw new QueueStateConflictException("Only a booked or called token can be marked as a no-show.");
        }
        BookingSnapshot before = snapshot(booking);
        booking.setStatus(BookingState.NO_SHOW);
        bookingRepository.save(booking);
        recordAudit(booking, booking.getSession(), actor, operationId, "NO_SHOW", before);
        return toResponse(booking);
    }

    @Transactional
    public StaffQueueBookingResponse createWalkIn(
            UUID staffId,
            UUID clinicId,
            CreateWalkInBookingRequest request
    ) {
        var actor = requireActor(staffId, clinicId);
        UUID operationId = UUID.randomUUID();
        ClinicSession session = clinicSessionRepository
                .findByIdAndClinic_IdForUpdate(request.sessionId(), clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
        if (bookingRepository.countActiveBySessionIdAndClinicId(session.getId(), clinicId)
                >= session.getMaxAppointments()) {
            throw new SessionFullException("This session is full.");
        }

        Booking booking = new Booking();
        booking.setSession(session);
        int maxTokenNo = bookingRepository.findMaxTokenNoBySessionIdAndClinicId(session.getId(), clinicId);
        if (maxTokenNo == Integer.MAX_VALUE) {
            throw new QueueStateConflictException("No additional token numbers are available.");
        }
        booking.setTokenNo(maxTokenNo + 1);
        booking.setQueueOrder(nextQueueOrder(
                bookingRepository.findMaxQueueOrderBySessionIdAndClinicId(session.getId(), clinicId)
        ));
        booking.setPatientName(request.patientName());
        booking.setPhone(request.phone());
        booking.setReason(request.reason());
        booking.setStatus(BookingState.BOOKED);
        booking.setSource(BookingSource.WALK_IN);
        bookingRepository.save(booking);
        recordAudit(booking, session, actor, operationId, "WALK_IN_CREATE", null);
        return toResponse(booking);
    }

    @Transactional
    public StaffQueueBookingResponse undo(UUID staffId, UUID clinicId, UUID bookingId) {
        var actor = requireActor(staffId, clinicId);
        Booking booking = findLockedBooking(clinicId, bookingId);
        List<AuditLog> bookingLogs = auditLogRepository.findBookingAuditLogForUpdate(clinicId, bookingId);
        AuditLog lastAction = bookingLogs.stream()
                .filter(log -> !"UNDO".equals(log.getAction()))
                .filter(log -> !log.isUndone())
                .findFirst()
                .orElseThrow(() -> new QueueStateConflictException("No queue action is available to undo."));
        UUID operationId = UUID.randomUUID();
        List<AuditLog> operationLogs = lastAction.getOperationId() == null
                ? List.of(lastAction)
                : auditLogRepository.findOperationAuditLogForUpdate(clinicId, lastAction.getOperationId());
        if (operationLogs.isEmpty()) {
            throw new QueueStateConflictException("The audited operation is unavailable for undo.");
        }
        Map<UUID, Booking> affectedBookings = new HashMap<>();
        Map<UUID, BookingSnapshot> beforeUndo = new HashMap<>();
        for (AuditLog actionLog : operationLogs) {
            if (actionLog.getEntityId() == null) {
                throw new QueueStateConflictException("The audit event does not reference a booking.");
            }
            Booking affectedBooking = actionLog.getEntityId().equals(bookingId)
                    ? booking
                    : bookingRepository.findByIdAndClinicIdForUpdate(actionLog.getEntityId(), clinicId)
                    .orElseThrow(() -> new NotFoundException("Booking not found."));
            if (!affectedBooking.getSession().getId().equals(booking.getSession().getId())
                    || actionLog.isUndone()
                    || !matchesAfterState(actionLog, affectedBooking)) {
                throw new QueueStateConflictException(
                        "A booking changed since the audited action; that action cannot be safely undone."
                );
            }
            affectedBookings.put(affectedBooking.getId(), affectedBooking);
            beforeUndo.put(affectedBooking.getId(), snapshot(affectedBooking));
        }

        for (AuditLog actionLog : operationLogs) {
            Booking affectedBooking = affectedBookings.get(actionLog.getEntityId());
            restoreBooking(affectedBooking, actionLog);
            bookingRepository.save(affectedBooking);
            actionLog.setUndone(true);
            auditLogRepository.save(actionLog);
            recordAudit(
                    affectedBooking,
                    affectedBooking.getSession(),
                    actor,
                    operationId,
                    "UNDO",
                    beforeUndo.get(affectedBooking.getId())
            );
        }
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<StaffAuditLogResponse> getAuditLog(UUID clinicId) {
        return auditLogRepository.findStaffQueueAuditLog(clinicId);
    }

    private ClinicSession lockOwnedSession(UUID sessionId, UUID clinicId) {
        return clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic session not found."));
    }

    private Booking findLockedBooking(UUID clinicId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndSession_Clinic_Id(bookingId, clinicId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        lockOwnedSession(booking.getSession().getId(), clinicId);
        return bookingRepository.findByIdAndClinicIdForUpdate(bookingId, clinicId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
    }

    private Booking callNextBooked(
            ClinicSession session,
            UUID clinicId,
            com.example.dentalbackend.entity.StaffUser actor,
            UUID operationId
    ) {
        UUID sessionId = session.getId();
        return bookingRepository.findBySessionIdAndStatusForUpdate(
                        sessionId,
                        clinicId,
                        BookingState.BOOKED
                )
                .stream()
                .findFirst()
                .map(booking -> {
                    BookingSnapshot before = snapshot(booking);
                    booking.setStatus(BookingState.SERVING);
                    booking.setStartedAt(Instant.now());
                    bookingRepository.save(booking);
                    recordAudit(booking, session, actor, operationId, "SKIP_AUTO_CALL", before);
                    return booking;
                })
                .orElse(null);
    }

    private com.example.dentalbackend.entity.StaffUser requireActor(UUID staffId, UUID clinicId) {
        return staffUserRepository.findByIdAndClinic_IdAndActiveTrue(staffId, clinicId)
                .orElseThrow(() -> new NotFoundException("Active staff member not found in this clinic."));
    }

    private BookingSnapshot snapshot(Booking booking) {
        return new BookingSnapshot(
                booking.getStatus(),
                booking.getQueueOrder(),
                booking.getSkipCount(),
                booking.getCalledAt(),
                booking.getStartedAt(),
                booking.getCompletedAt()
        );
    }

    private boolean matchesAfterState(AuditLog log, Booking booking) {
        return log.getAfterStatus() == booking.getStatus()
                && Objects.equals(log.getQueueOrderAfter(), booking.getQueueOrder())
                && Objects.equals(log.getSkipCountAfter(), booking.getSkipCount())
                && Objects.equals(log.getCalledAtAfter(), booking.getCalledAt())
                && Objects.equals(log.getStartedAtAfter(), booking.getStartedAt())
                && Objects.equals(log.getCompletedAtAfter(), booking.getCompletedAt());
    }

    private void recordAudit(
            Booking booking,
            ClinicSession session,
            com.example.dentalbackend.entity.StaffUser actor,
            UUID operationId,
            String action,
            BookingSnapshot before
    ) {
        AuditLog log = new AuditLog();
        log.setClinic(session.getClinic());
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType("BOOKING");
        log.setEntityId(booking.getId());
        log.setOperationId(operationId);
        log.setTokenNo(booking.getTokenNo());
        log.setBeforeStatus(before == null ? null : before.status());
        log.setAfterStatus(booking.getStatus());
        log.setQueueOrderBefore(before == null ? null : before.queueOrder());
        log.setQueueOrderAfter(booking.getQueueOrder());
        log.setSkipCountBefore(before == null ? null : before.skipCount());
        log.setSkipCountAfter(booking.getSkipCount());
        log.setCalledAtBefore(before == null ? null : before.calledAt());
        log.setCalledAtAfter(booking.getCalledAt());
        log.setStartedAtBefore(before == null ? null : before.startedAt());
        log.setStartedAtAfter(booking.getStartedAt());
        log.setCompletedAtBefore(before == null ? null : before.completedAt());
        log.setCompletedAtAfter(booking.getCompletedAt());
        auditLogRepository.save(log);
    }

    private void restoreBooking(Booking booking, AuditLog actionLog) {
        booking.setStatus(actionLog.getBeforeStatus() == null
                ? BookingState.CANCELLED
                : actionLog.getBeforeStatus());
        if (actionLog.getQueueOrderBefore() != null) {
            booking.setQueueOrder(actionLog.getQueueOrderBefore());
        }
        if (actionLog.getSkipCountBefore() != null) {
            booking.setSkipCount(actionLog.getSkipCountBefore());
        }
        booking.setCalledAt(actionLog.getCalledAtBefore());
        booking.setStartedAt(actionLog.getStartedAtBefore());
        booking.setCompletedAt(actionLog.getCompletedAtBefore());
    }

    private record BookingSnapshot(
            BookingState status,
            int queueOrder,
            int skipCount,
            Instant calledAt,
            Instant startedAt,
            Instant completedAt
    ) {
    }

    private int indexOfBooking(List<Booking> bookings, UUID bookingId) {
        for (int index = 0; index < bookings.size(); index++) {
            if (bookings.get(index).getId().equals(bookingId)) {
                return index;
            }
        }
        throw new IllegalStateException("Serving booking was not found in the locked session queue.");
    }

    private int nextQueueOrder(int currentMax) {
        if (currentMax > Integer.MAX_VALUE - 10) {
            throw new QueueStateConflictException("No additional queue positions are available.");
        }
        return currentMax + 10;
    }

    private StaffQueueBookingResponse toResponse(Booking booking) {
        return new StaffQueueBookingResponse(
                booking.getId(),
                booking.getTokenNo(),
                booking.getQueueOrder(),
                booking.getPatientName(),
                booking.getPhone(),
                booking.getReason(),
                booking.getStatus(),
                booking.getCalledAt(),
                booking.getStartedAt(),
                booking.getCompletedAt()
        );
    }
}
