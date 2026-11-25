package com.hospital.patientadmission;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatientAdmissionApplication {

    public static void main(String[] args) {
        // "Admission date cannot be in the future" is judged against the hospital's local date,
        // not the server's (cloud hosts usually run in UTC). Override with APP_TIMEZONE.
        String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(PatientAdmissionApplication.class, args);
    }
}
