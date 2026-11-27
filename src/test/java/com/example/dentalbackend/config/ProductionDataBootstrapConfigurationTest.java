package com.example.dentalbackend.config;

import com.example.dentalbackend.entity.BookingStatus;
import com.example.dentalbackend.entity.Clinic;
import com.example.dentalbackend.entity.ClinicSession;
import com.example.dentalbackend.entity.SessionType;
import com.example.dentalbackend.entity.StaffRole;
import com.example.dentalbackend.entity.StaffUser;
import com.example.dentalbackend.repository.ClinicRepository;
import com.example.dentalbackend.repository.ClinicSessionRepository;
import com.example.dentalbackend.repository.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentCaptor.forClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionDataBootstrapConfigurationTest {

    @Test
    void createsInitialClinicDoctorAndSessionsWhenDatabaseIsEmpty() {
        ClinicRepository clinicRepository = mock(ClinicRepository.class);
        StaffUserRepository staffUserRepository = mock(StaffUserRepository.class);
        ClinicSessionRepository clinicSessionRepository = mock(ClinicSessionRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MockEnvironment environment = configuredEnvironment();

        when(clinicRepository.count()).thenReturn(0L);
        when(clinicRepository.save(any(Clinic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffUserRepository.save(any(StaffUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clinicSessionRepository.save(any(ClinicSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new ProductionDataBootstrapConfiguration.ProductionDataBootstrapper(
                clinicRepository,
                staffUserRepository,
                clinicSessionRepository,
                passwordEncoder,
                environment
        ).seed();

        var clinicCaptor = forClass(Clinic.class);
        verify(clinicRepository).save(clinicCaptor.capture());
    }
}
//         verify(clinicRepository).save(clinicCaptThink of it like this: your Render database is empty, like a new notebook. The app looks for a clinic, doesn't find one, and says "Clinic not found". You need to write a few starting entries (a clinic, a doctor login, and morning and evening sessions) into that notebook.
// There are two ways to do it. The first is easier.

// Way 1: Let your Spring Boot code add the data (easiest)
// Create a new file in your backend, for example DataSeeder.java.
// In it, write code that says: "When the app starts, if there is no clinic, create one clinic, one doctor user, and two sessions."
// Push the code to GitHub. Render redeploys automatically.
// When the backend starts on Render, the data is added to the database by itself.or.capture());
//         assertEquals("Sample Clinic", clinicCaptor.getValue().getName());
//         assertEquals("sample-clinic", clinicCaptor.getValue().getSlug());

//         var doctorCaptor = forClass(StaffUser.class);
//         verify(staffUserRepository).save(doctorCaptor.capture());
//         StaffUser doctor = doctorCaptor.getValue();
//         assertEquals("doctor@example.com", doctor.getEmail());
//         assertEquals(StaffRole.DOCTOR, doctor.getRole());
//         assertTrue(passwordEncoder.matches("production-pass-123", doctor.getPasswordHash()));

//         var sessionsCaptor = forClass(ClinicSession.class);
//         verify(clinicSessionRepository, times(2)).save(sessionsCaptor.capture());
//         List<ClinicSession> sessions = sessionsCaptor.getAllValues();
//         assertEquals(List.of(SessionType.MORNING, SessionType.EVENING),
//                 sessions.stream().map(ClinicSession::getType).toList());
//         assertTrue(sessions.stream().allMatch(session ->
//                 session.getDate().equals(LocalDate.now(ZoneId.of("Asia/Kolkata")))
//                         && session.getClinic() == clinicCaptor.getValue()
//                         && session.getDoctor() == doctor
//                         && session.getMaxAppointments() == 20
//                         && session.getAvgMinutes() == 15));
//         assertEquals(BookingStatus.OPEN, sessions.get(1).getBookingStatus());
//     }

//     @Test
//     void skipsBootstrapWhenAClinicAlreadyExists() {
//         ClinicRepository clinicRepository = mock(ClinicRepository.class);
//         StaffUserRepository staffUserRepository = mock(StaffUserRepository.class);
//         ClinicSessionRepository clinicSessionRepository = mock(ClinicSessionRepository.class);

//         when(clinicRepository.count()).thenReturn(1L);

//         new ProductionDataBootstrapConfiguration.ProductionDataBootstrapper(
//                 clinicRepository,
//                 staffUserRepository,
//                 clinicSessionRepository,
//                 new BCryptPasswordEncoder(),
//                 new MockEnvironment()
//         ).seed();

//         verify(clinicRepository, never()).save(any(Clinic.class));
//         verify(staffUserRepository, never()).save(any(StaffUser.class));
//         verify(clinicSessionRepository, never()).save(any(ClinicSession.class));
//     }

//     private MockEnvironment configuredEnvironment() {
//         return new MockEnvironment()
//                 .withProperty("app.bootstrap.clinic-name", "Sample Clinic")
//                 .withProperty("app.bootstrap.clinic-slug", "sample-clinic")
//                 .withProperty("app.bootstrap.doctor-name", "Sample Doctor")
//                 .withProperty("app.bootstrap.doctor-email", "Doctor@Example.com")
//                 .withProperty("app.bootstrap.doctor-password", "production-pass-123");
//     }
// }
