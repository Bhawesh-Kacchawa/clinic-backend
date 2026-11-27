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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

@Configuration
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class ProductionDataBootstrapConfiguration {

    @Bean
    CommandLineRunner bootstrapInitialClinic(
            ClinicRepository clinicRepository,
            StaffUserRepository staffUserRepository,
            ClinicSessionRepository clinicSessionRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            PlatformTransactionManager transactionManager
    ) {
        ProductionDataBootstrapper bootstrapper = new ProductionDataBootstrapper(
                clinicRepository,
                staffUserRepository,
                clinicSessionRepository,
                passwordEncoder,
                environment
        );
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return args -> transactionTemplate.executeWithoutResult(status -> bootstrapper.seed());
    }

    static class ProductionDataBootstrapper {

        private static final Logger log = LoggerFactory.getLogger(ProductionDataBootstrapper.class);
        private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

        private final ClinicRepository clinicRepository;
        private final StaffUserRepository staffUserRepository;
        private final ClinicSessionRepository clinicSessionRepository;
        private final PasswordEncoder passwordEncoder;
        private final Environment environment;

        ProductionDataBootstrapper(
                ClinicRepository clinicRepository,
                StaffUserRepository staffUserRepository,
                ClinicSessionRepository clinicSessionRepository,
                PasswordEncoder passwordEncoder,
                Environment environment
        ) {
            this.clinicRepository = clinicRepository;
            this.staffUserRepository = staffUserRepository;
            this.clinicSessionRepository = clinicSessionRepository;
            this.passwordEncoder = passwordEncoder;
            this.environment = environment;
        }

        void seed() {
            if (clinicRepository.count() > 0) {
                log.info("Production bootstrap skipped because a clinic already exists.");
                return;
            }

            String clinicName = required("app.bootstrap.clinic-name");
            String clinicSlug = required("app.bootstrap.clinic-slug");
            String doctorName = required("app.bootstrap.doctor-name");
            String doctorEmail = required("app.bootstrap.doctor-email").toLowerCase(Locale.ROOT);
            String doctorPassword = required("app.bootstrap.doctor-password");
            validateLengths(clinicName, clinicSlug, doctorName, doctorEmail, doctorPassword);

            Clinic clinic = new Clinic();
            clinic.setName(clinicName);
            clinic.setSlug(clinicSlug);
            clinic = clinicRepository.save(clinic);

            StaffUser doctor = new StaffUser();
            doctor.setClinic(clinic);
            doctor.setFullName(doctorName);
            doctor.setEmail(doctorEmail);
            doctor.setPasswordHash(passwordEncoder.encode(doctorPassword));
            doctor.setRole(StaffRole.DOCTOR);
            doctor = staffUserRepository.save(doctor);

            LocalDate today = LocalDate.now(CLINIC_ZONE);
            boolean afterNoon = !LocalTime.now(CLINIC_ZONE).isBefore(LocalTime.NOON);
            seedSession(clinic, doctor, today, SessionType.MORNING,
                    LocalTime.of(9, 0), LocalTime.of(13, 0),
                    afterNoon ? BookingStatus.CLOSED : BookingStatus.OPEN);
            seedSession(clinic, doctor, today, SessionType.EVENING,
                    LocalTime.of(17, 0), LocalTime.of(21, 0), BookingStatus.OPEN);

            log.info("Production bootstrap created initial clinic and doctor data for clinic '{}'.", clinicSlug);
        }

        private String required(String propertyName) {
            String value = environment.getProperty(propertyName);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "Production bootstrap is enabled, but required property '" + propertyName + "' is missing."
                );
            }
            return value.trim();
        }

        private void validateLengths(
                String clinicName,
                String clinicSlug,
                String doctorName,
                String doctorEmail,
                String doctorPassword
        ) {
            requireMaximumLength("clinic name", clinicName, 150);
            requireMaximumLength("clinic slug", clinicSlug, 100);
            requireMaximumLength("doctor name", doctorName, 150);
            requireMaximumLength("doctor email", doctorEmail, 254);
            int passwordBytes = doctorPassword.getBytes(StandardCharsets.UTF_8).length;
            if (doctorPassword.length() < 12 || passwordBytes > 72) {
                throw new IllegalStateException(
                        "Bootstrap doctor password must be at least 12 characters and no more than 72 UTF-8 bytes."
                );
            }
        }

        private void requireMaximumLength(String label, String value, int maximumLength) {
            if (value.length() > maximumLength) {
                throw new IllegalStateException(
                        "Bootstrap " + label + " must be no more than " + maximumLength + " characters."
                );
            }
        }

        private void seedSession(
                Clinic clinic,
                StaffUser doctor,
                LocalDate date,
                SessionType type,
                LocalTime startTime,
                LocalTime endTime,
                BookingStatus bookingStatus
        ) {
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
            clinicSessionRepository.save(session);
        }
    }
}
