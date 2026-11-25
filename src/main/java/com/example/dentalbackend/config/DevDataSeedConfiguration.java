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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
@Profile("dev")
public class DevDataSeedConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeedConfiguration.class);
    private static final String CLINIC_SLUG = "demo-dental";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    CommandLineRunner seedDevData(
            ClinicRepository clinicRepository,
            StaffUserRepository staffUserRepository,
            ClinicSessionRepository clinicSessionRepository,
            PlatformTransactionManager transactionManager,
            PasswordEncoder passwordEncoder,
            @Value("${demo.doctor.email}") String doctorEmail,
            @Value("${demo.doctor.password}") String doctorPassword
    ) {
        DevDataSeeder seeder = new DevDataSeeder(
                clinicRepository,
                staffUserRepository,
                clinicSessionRepository,
                passwordEncoder,
                doctorEmail,
                doctorPassword
        );
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return args -> transactionTemplate.executeWithoutResult(status -> seeder.seed());
    }

    public static class DevDataSeeder {

        private final ClinicRepository clinicRepository;
        private final StaffUserRepository staffUserRepository;
        private final ClinicSessionRepository clinicSessionRepository;
        private final PasswordEncoder passwordEncoder;
        private final String doctorEmail;
        private final String doctorPassword;

        public DevDataSeeder(
                ClinicRepository clinicRepository,
                StaffUserRepository staffUserRepository,
                ClinicSessionRepository clinicSessionRepository,
                PasswordEncoder passwordEncoder,
                String doctorEmail,
                String doctorPassword
        ) {
            this.clinicRepository = clinicRepository;
            this.staffUserRepository = staffUserRepository;
            this.clinicSessionRepository = clinicSessionRepository;
            this.passwordEncoder = passwordEncoder;
            this.doctorEmail = doctorEmail;
            this.doctorPassword = doctorPassword;
        }

        public void seed() {
            Clinic clinic = clinicRepository.findBySlug(CLINIC_SLUG)
                    .orElseGet(this::createClinic);
            StaffUser doctor = staffUserRepository.findByEmailIgnoreCase(doctorEmail)
                    .map(existingDoctor -> {
                        if (!existingDoctor.getClinic().getId().equals(clinic.getId())
                                || existingDoctor.getRole() != StaffRole.DOCTOR) {
                            throw new IllegalStateException(
                                    "Configured demo doctor email is already assigned to another account.");
                        }
                        return existingDoctor;
                    })
                    .orElseGet(() -> createDoctor(clinic));

            LocalDate today = LocalDate.now(CLINIC_ZONE);
            boolean afterNoon = !LocalTime.now(CLINIC_ZONE).isBefore(LocalTime.NOON);
            seedSession(clinicSessionRepository, clinic, doctor, today, SessionType.MORNING,
                    LocalTime.of(9, 0), LocalTime.of(13, 0),
                    afterNoon ? BookingStatus.CLOSED : BookingStatus.OPEN);
            seedSession(clinicSessionRepository, clinic, doctor, today, SessionType.EVENING,
                    LocalTime.of(17, 0), LocalTime.of(21, 0), BookingStatus.OPEN);

            log.info("Development seed data ensured for clinic '{}' and doctor '{}'.", CLINIC_SLUG, doctorEmail);
        }

        private Clinic createClinic() {
            Clinic clinic = new Clinic();
            clinic.setName("Demo Dental");
            clinic.setSlug(CLINIC_SLUG);
            return clinicRepository.save(clinic);
        }

        private StaffUser createDoctor(Clinic clinic) {
            StaffUser doctor = new StaffUser();
            doctor.setClinic(clinic);
            doctor.setFullName("Demo Doctor");
            doctor.setEmail(doctorEmail);
            doctor.setPasswordHash(passwordEncoder.encode(doctorPassword));
            doctor.setRole(StaffRole.DOCTOR);
            return staffUserRepository.save(doctor);
        }

        private void seedSession(
                ClinicSessionRepository repository,
                Clinic clinic,
                StaffUser doctor,
                LocalDate date,
                SessionType type,
                LocalTime startTime,
                LocalTime endTime,
                BookingStatus bookingStatus
        ) {
            Optional<ClinicSession> existingSession = repository.findByClinic_IdAndDateAndType(
                    clinic.getId(),
                    date,
                    type
            );
            if (existingSession.isPresent()) {
                return;
            }

            ClinicSession session = new ClinicSession();
            session.setClinic(clinic);
            session.setDoctor(doctor);
            session.setType(type);
            session.setDate(date);
            session.setStartTime(startTime);
            session.setEndTime(endTime);
            session.setMaxAppointments(20);
            session.setBookingOpensAt(date.atStartOfDay(CLINIC_ZONE).toInstant());
            session.setBookingStatus(bookingStatus);
            session.setAvgMinutes(15);
            repository.save(session);
        }
    }
}
