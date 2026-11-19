package com.example.dentalbackend.service;

import com.example.dentalbackend.dto.CallNextResponse;
import com.example.dentalbackend.dto.ClinicCallNextResponse;
import com.example.dentalbackend.dto.CreateWalkInBookingRequest;
import com.example.dentalbackend.entity.AuditLog;
import com.example.dentalbackend.entity.Booking;
import com.example.dentalbackend.entity.BookingSource;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.repository.AuditLogRepository;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StaffQueueServiceTest {

    private static final UUID STAFF_ID = UUID.randomUUID();

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ClinicSessionRepository clinicSessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StaffUserRepository staffUserRepository;

    @InjectMocks
    private StaffQueueService staffQueueService;

    @BeforeEach
    void setUpActor() {
        StaffUser staffUser = new StaffUser();
        staffUser.setId(STAFF_ID);
        staffUser.setClinic(new Clinic());
        lenient().when(staffUserRepository.findByIdAndClinic_IdAndActiveTrue(
                eq(STAFF_ID),
                any(UUID.class)
        )).thenReturn(Optional.of(staffUser));
    }

    @Test
    void callNextCompletesCurrentBookingAndStartsNextBookedBooking() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking current = booking(sessionId, 1, BookingState.SERVING);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.SERVING))
                .thenReturn(List.of(current));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.BOOKED))
                .thenReturn(List.of(next));

        CallNextResponse response = staffQueueService.callNext(STAFF_ID, clinicId, sessionId);

        assertEquals(BookingState.DONE, current.getStatus());
        assertNotNull(current.getCompletedAt());
        assertEquals(BookingState.SERVING, next.getStatus());
        assertNotNull(next.getStartedAt());
        assertEquals(current.getId(), response.completedBooking().id());
        assertEquals(next.getId(), response.servingBooking().id());
        assertFalse(response.queueEmpty());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(auditCaptor.capture());
        assertEquals(BookingState.SERVING, auditCaptor.getAllValues().get(0).getBeforeStatus());
        assertEquals(BookingState.DONE, auditCaptor.getAllValues().get(0).getAfterStatus());
        assertEquals(BookingState.BOOKED, auditCaptor.getAllValues().get(1).getBeforeStatus());
        assertEquals(BookingState.SERVING, auditCaptor.getAllValues().get(1).getAfterStatus());
        assertEquals(
                auditCaptor.getAllValues().get(0).getOperationId(),
                auditCaptor.getAllValues().get(1).getOperationId()
        );
        verify(bookingRepository).findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.SERVING
        );
        verify(bookingRepository).findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.BOOKED
        );
    }

    @Test
    void callNextHandlesEmptyQueue() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                eq(sessionId),
                eq(clinicId),
                eq(BookingState.SERVING)
        )).thenReturn(List.of());
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                eq(sessionId),
                eq(clinicId),
                eq(BookingState.BOOKED)
        )).thenReturn(List.of());

        CallNextResponse response = staffQueueService.callNext(STAFF_ID, clinicId, sessionId);

        assertNull(response.completedBooking());
        assertNull(response.servingBooking());
        assertTrue(response.queueEmpty());
    }

    @Test
    void callNextByClinicAndSessionTypeReturnsUpdatedQueue() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking current = booking(sessionId, 1, BookingState.SERVING);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        ClinicSession session = session(sessionId);
        when(clinicSessionRepository.findByClinicIdAndSlugAndDateAndTypeForUpdate(
                eq(clinicId),
                eq("demo-dental"),
                any(),
                eq(SessionType.EVENING)
        )).thenReturn(Optional.of(session));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.SERVING))
                .thenReturn(List.of(current));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(sessionId, clinicId, BookingState.BOOKED))
                .thenReturn(List.of(next));
        when(bookingRepository.findBySession_IdAndSession_Clinic_IdOrderByQueueOrderAsc(sessionId, clinicId))
                .thenReturn(List.of(current, next));

        ClinicCallNextResponse response = staffQueueService.callNextForToday(
                STAFF_ID,
                clinicId,
                "demo-dental",
                SessionType.EVENING
        );

        assertEquals(sessionId, response.sessionId());
        assertEquals(current.getId(), response.completedBooking().id());
        assertEquals(next.getId(), response.servingBooking().id());
        assertEquals(List.of(BookingState.DONE, BookingState.SERVING),
                response.queue().stream().map(item -> item.status()).toList());
        assertFalse(response.queueEmpty());
    }

    @Test
    void skippingServingBookingMarksSkippedAndStartsNextBookedBooking() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking skipped = booking(sessionId, 1, BookingState.SERVING);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        when(bookingRepository.findByIdAndSession_Clinic_Id(skipped.getId(), clinicId))
                .thenReturn(Optional.of(skipped));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(skipped.getId(), clinicId))
                .thenReturn(Optional.of(skipped));
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.SERVING
        )).thenReturn(List.of());
        when(bookingRepository.findBySessionIdAndStatusForUpdate(
                sessionId,
                clinicId,
                BookingState.BOOKED
        )).thenReturn(List.of(next));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffQueueService.skip(STAFF_ID, clinicId, skipped.getId());

        assertEquals(BookingState.SKIPPED, skipped.getStatus());
        assertEquals(1, skipped.getSkipCount());
        assertEquals(BookingState.SERVING, next.getStatus());
        assertNotNull(next.getStartedAt());
        assertEquals(next.getId(), response.servingBooking().id());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(auditCaptor.capture());
        assertEquals("SKIP", auditCaptor.getAllValues().get(0).getAction());
        assertEquals("SKIP_AUTO_CALL", auditCaptor.getAllValues().get(1).getAction());
        assertEquals(
                auditCaptor.getAllValues().get(0).getOperationId(),
                auditCaptor.getAllValues().get(1).getOperationId()
        );
    }

    @Test
    void requeuePlacesSkippedBookingMidpointAfterServingWithoutChangingToken() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking serving = booking(sessionId, 1, BookingState.SERVING);
        serving.setQueueOrder(10);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        next.setQueueOrder(30);
        Booking requeued = booking(sessionId, 3, BookingState.SKIPPED);
        requeued.setQueueOrder(50);
        when(bookingRepository.findByIdAndSession_Clinic_Id(requeued.getId(), clinicId))
                .thenReturn(Optional.of(requeued));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(requeued.getId(), clinicId))
                .thenReturn(Optional.of(requeued));
        when(bookingRepository.findQueueBySessionIdAndClinicIdForUpdate(sessionId, clinicId))
                .thenReturn(List.of(serving, next, requeued));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int tokenNo = requeued.getTokenNo();
        var response = staffQueueService.requeue(STAFF_ID, clinicId, requeued.getId());

        assertEquals(BookingState.BOOKED, requeued.getStatus());
        assertEquals(tokenNo, requeued.getTokenNo());
        assertEquals(20, requeued.getQueueOrder());
        assertEquals(requeued.getId(), response.id());
    }

    @Test
    void requeueRebalancesWhenThereIsNoIntegerMidpoint() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking serving = booking(sessionId, 1, BookingState.SERVING);
        serving.setQueueOrder(10);
        Booking next = booking(sessionId, 2, BookingState.BOOKED);
        next.setQueueOrder(11);
        Booking requeued = booking(sessionId, 3, BookingState.SKIPPED);
        requeued.setQueueOrder(20);
        when(bookingRepository.findByIdAndSession_Clinic_Id(requeued.getId(), clinicId))
                .thenReturn(Optional.of(requeued));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(requeued.getId(), clinicId))
                .thenReturn(Optional.of(requeued));
        when(bookingRepository.findQueueBySessionIdAndClinicIdForUpdate(sessionId, clinicId))
                .thenReturn(List.of(serving, next, requeued));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        staffQueueService.requeue(STAFF_ID, clinicId, requeued.getId());

        assertEquals(10, serving.getQueueOrder());
        assertEquals(20, requeued.getQueueOrder());
        assertEquals(30, next.getQueueOrder());
    }

    @Test
    void noShowMovesBookedTokenToNoShow() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking booking = booking(sessionId, 1, BookingState.BOOKED);
        when(bookingRepository.findByIdAndSession_Clinic_Id(booking.getId(), clinicId))
                .thenReturn(Optional.of(booking));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(booking.getId(), clinicId))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffQueueService.markNoShow(STAFF_ID, clinicId, booking.getId());

        assertEquals(BookingState.NO_SHOW, response.status());
        assertEquals(BookingState.NO_SHOW, booking.getStatus());
    }

    @Test
    void undoRestoresLastBookingActionAndWritesUndoAuditEntry() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Booking booking = booking(sessionId, 1, BookingState.CALLED);
        booking.setQueueOrder(20);
        booking.setCalledAt(java.time.Instant.parse("2026-01-01T10:00:00Z"));
        AuditLog lastAction = new AuditLog();
        lastAction.setEntityId(booking.getId());
        lastAction.setAction("CALL");
        lastAction.setBeforeStatus(BookingState.BOOKED);
        lastAction.setAfterStatus(BookingState.CALLED);
        lastAction.setQueueOrderBefore(20);
        lastAction.setQueueOrderAfter(20);
        lastAction.setSkipCountBefore(0);
        lastAction.setSkipCountAfter(0);
        lastAction.setCalledAtBefore(null);
        lastAction.setCalledAtAfter(booking.getCalledAt());
        lastAction.setStartedAtBefore(null);
        lastAction.setStartedAtAfter(null);
        lastAction.setCompletedAtBefore(null);
        lastAction.setCompletedAtAfter(null);

        when(bookingRepository.findByIdAndSession_Clinic_Id(booking.getId(), clinicId))
                .thenReturn(Optional.of(booking));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(booking.getId(), clinicId))
                .thenReturn(Optional.of(booking));
        when(auditLogRepository.findBookingAuditLogForUpdate(clinicId, booking.getId()))
                .thenReturn(List.of(lastAction));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffQueueService.undo(STAFF_ID, clinicId, booking.getId());

        assertEquals(BookingState.BOOKED, booking.getStatus());
        assertNull(booking.getCalledAt());
        assertTrue(lastAction.isUndone());
        assertEquals(BookingState.BOOKED, response.status());
        verify(auditLogRepository).save(lastAction);
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.argThat(
                log -> "UNDO".equals(log.getAction())
                        && log.getBeforeStatus() == BookingState.CALLED
                        && log.getAfterStatus() == BookingState.BOOKED
        ));
    }

    @Test
    void undoingSkipAlsoRevertsTheAutomaticCallFromThatQueueAction() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        Booking skipped = booking(sessionId, 1, BookingState.SKIPPED);
        skipped.setSkipCount(1);
        Booking autoCalled = booking(sessionId, 2, BookingState.SERVING);
        autoCalled.setStartedAt(java.time.Instant.parse("2026-01-01T10:00:00Z"));

        AuditLog skipLog = auditLog(
                operationId,
                skipped,
                "SKIP",
                BookingState.SERVING,
                BookingState.SKIPPED,
                0,
                1,
                null,
                null
        );
        AuditLog autoCallLog = auditLog(
                operationId,
                autoCalled,
                "SKIP_AUTO_CALL",
                BookingState.BOOKED,
                BookingState.SERVING,
                0,
                0,
                null,
                autoCalled.getStartedAt()
        );

        when(bookingRepository.findByIdAndSession_Clinic_Id(skipped.getId(), clinicId))
                .thenReturn(Optional.of(skipped));
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session(sessionId)));
        when(bookingRepository.findByIdAndClinicIdForUpdate(skipped.getId(), clinicId))
                .thenReturn(Optional.of(skipped));
        when(bookingRepository.findByIdAndClinicIdForUpdate(autoCalled.getId(), clinicId))
                .thenReturn(Optional.of(autoCalled));
        when(auditLogRepository.findBookingAuditLogForUpdate(clinicId, skipped.getId()))
                .thenReturn(List.of(skipLog));
        when(auditLogRepository.findOperationAuditLogForUpdate(clinicId, operationId))
                .thenReturn(List.of(skipLog, autoCallLog));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        staffQueueService.undo(STAFF_ID, clinicId, skipped.getId());

        assertEquals(BookingState.SERVING, skipped.getStatus());
        assertEquals(0, skipped.getSkipCount());
        assertEquals(BookingState.BOOKED, autoCalled.getStatus());
        assertNull(autoCalled.getStartedAt());
        assertTrue(skipLog.isUndone());
        assertTrue(autoCallLog.isUndone());
    }

    @Test
    void walkInGetsNextTokenAndIsAppendedToQueue() {
        UUID clinicId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClinicSession session = session(sessionId);
        session.setMaxAppointments(10);
        when(clinicSessionRepository.findByIdAndClinic_IdForUpdate(sessionId, clinicId))
                .thenReturn(Optional.of(session));
        when(bookingRepository.countActiveBySessionIdAndClinicId(sessionId, clinicId)).thenReturn(2L);
        when(bookingRepository.findMaxTokenNoBySessionIdAndClinicId(sessionId, clinicId)).thenReturn(2);
        when(bookingRepository.findMaxQueueOrderBySessionIdAndClinicId(sessionId, clinicId)).thenReturn(20);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffQueueService.createWalkIn(
                STAFF_ID,
                clinicId,
                new CreateWalkInBookingRequest(
                        sessionId,
                        "Walk-in patient",
                        "5550003",
                        "34 Example Avenue"
                )
        );

        assertEquals(3, response.tokenNo());
        assertEquals(30, response.queueOrder());
        assertEquals("34 Example Avenue", response.address());
        assertEquals(BookingState.BOOKED, response.status());
        verify(bookingRepository).save(org.mockito.ArgumentMatchers.argThat(
                booking -> booking.getSource() == BookingSource.WALK_IN
                        && "34 Example Avenue".equals(booking.getAddress())
        ));
    }

    private Booking booking(UUID sessionId, int token, BookingState state) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setSession(session(sessionId));
        booking.setTokenNo(token);
        booking.setQueueOrder(token * 10);
        booking.setPatientName("Patient " + token);
        booking.setPhone("555000" + token);
        booking.setStatus(state);
        return booking;
    }

    private AuditLog auditLog(
            UUID operationId,
            Booking booking,
            String action,
            BookingState beforeStatus,
            BookingState afterStatus,
            int beforeSkipCount,
            int afterSkipCount,
            java.time.Instant beforeStartedAt,
            java.time.Instant afterStartedAt
    ) {
        AuditLog log = new AuditLog();
        log.setOperationId(operationId);
        log.setEntityId(booking.getId());
        log.setAction(action);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setQueueOrderBefore(booking.getQueueOrder());
        log.setQueueOrderAfter(booking.getQueueOrder());
        log.setSkipCountBefore(beforeSkipCount);
        log.setSkipCountAfter(afterSkipCount);
        log.setCalledAtBefore(null);
        log.setCalledAtAfter(null);
        log.setStartedAtBefore(beforeStartedAt);
        log.setStartedAtAfter(afterStartedAt);
        log.setCompletedAtBefore(null);
        log.setCompletedAtAfter(null);
        return log;
    }

    private ClinicSession session(UUID id) {
        ClinicSession session = new ClinicSession();
        session.setId(id);
        return session;
    }
}
