package com.hospital.patientadmission.domain;

import java.util.Arrays;
import java.util.Optional;

public enum PatientStatus {
    ADMITTED("Admitted"),
    DISCHARGED("Discharged");

    private final String label;

    PatientStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<PatientStatus> fromLabel(String value) {
        if(value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return Arrays.stream(PatientStatus.values())
                .filter(status -> status.label.equalsIgnoreCase(trimmed))
                .findFirst();
    }
}
