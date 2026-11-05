package com.example.dentalbackend;

import com.example.dentalbackend.dto.CreatePublicBookingRequest;
import com.example.dentalbackend.dto.PublicBookingResponse;
import com.example.dentalbackend.dto.PublicBookingStatusResponse;
import com.example.dentalbackend.entity.BookingState;
import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.entity.StaffRole;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.repository.BookingRepository;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicBookingIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private ClinicSessionRepository clinicSessionRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @LocalServerPort
    private int port;

    private UUID sessionId;
    private LocalDate sessionDate;

    @BeforeEach
    void createOpenSessionWithFiveSlots() {
        bookingRepository.deleteAll();
        clinicSessionRepository.deleteAll();
        staffUserRepository.deleteAll();
        clinicRepository.deleteAll();

        Clinic clinic = new Clinic();
        clinic.setName("Concurrency Test Clinic");
        clinic.setSlug("concurrency-" + UUID.randomUUID());
        clinic = clinicRepository.save(clinic);

        StaffUser doctor = new StaffUser();
        doctor.setClinic(clinic);
        doctor.setFullName("Test Doctor");
        doctor.setEmail(UUID.randomUUID() + "@test.local");
        doctor.setPasswordHash("test-password-hash");
        doctor.setRole(StaffRole.DOCTOR);
        doctor = staffUserRepository.save(doctor);

        ClinicSession session = new ClinicSession();
        session.setClinic(clinic);
        session.setDoctor(doctor);
        session.setType(SessionType.MORNING);
        sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        session.setDate(sessionDate);
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(13, 0));
        session.setMaxAppointments(5);
        session.setBookingOpensAt(Instant.now().minusSeconds(60));
        session.setBookingStatus(BookingStatus.OPEN);
        session.setAvgMinutes(15);
        sessionId = clinicSessionRepository.save(session).getId();
    }

    @Test
    void concurrentRequestsOnlyBookAvailableSlotsWithConsecutiveTokens() throws Exception {
        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<PublicBookingResponse>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                int requestNumber = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    CreatePublicBookingRequest request = new CreatePublicBookingRequest(
                            sessionId,
                            "Patient " + requestNumber,
                            "555000" + requestNumber,
                            null
                    );
                    return restTemplate.postForEntity(
                            "http://localhost:" + port + "/api/public/bookings",
                            request,
                            PublicBookingResponse.class
                    );
                }));
            }

            ready.await();
            start.countDown();

            List<PublicBookingResponse> successfulBookings = new ArrayList<>();
            int conflictCount = 0;
            for (Future<ResponseEntity<PublicBookingResponse>> future : futures) {
                ResponseEntity<PublicBookingResponse> response = future.get();
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    assertNotNull(response.getBody());
                    successfulBookings.add(response.getBody());
                } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                    conflictCount++;
                }
            }

            assertEquals(5, successfulBookings.size());
            assertEquals(15, conflictCount);
            assertEquals(
                    5,
                    successfulBookings.stream()
                            .map(PublicBookingResponse::bookingId)
                            .distinct()
                            .count()
            );
            assertEquals(
                    List.of(1, 2, 3, 4, 5),
                    successfulBookings.stream()
                            .map(PublicBookingResponse::tokenNo)
                            .sorted()
                            .toList()
            );
            assertEquals(5, bookingRepository.count());

            PublicBookingResponse fifthBooking = successfulBookings.stream()
                    .filter(booking -> booking.tokenNo() == 5)
                    .findFirst()
                    .orElseThrow();
            ResponseEntity<PublicBookingStatusResponse> statusResponse = restTemplate.getForEntity(
                    "http://localhost:" + port + "/api/public/bookings/" + fifthBooking.bookingId(),
                    PublicBookingStatusResponse.class
            );
            assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
            assertNotNull(statusResponse.getBody());
            assertEquals(5, statusResponse.getBody().token());
            assertEquals(BookingState.BOOKED, statusResponse.getBody().status());
            assertNull(statusResponse.getBody().currentServingToken());
            assertEquals(4, statusResponse.getBody().patientsAhead());
            assertEquals(
                    LocalDateTime.of(sessionDate, LocalTime.of(10, 0)),
                    statusResponse.getBody().estimatedTime()
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
