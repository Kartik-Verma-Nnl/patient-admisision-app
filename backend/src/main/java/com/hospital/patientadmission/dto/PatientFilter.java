package com.hospital.patientadmission.dto;

import java.util.Locale;

import com.hospital.patientadmission.domain.PatientStatus;
import com.hospital.patientadmission.domain.ReferenceData;
import com.hospital.patientadmission.domain.ReferenceData.Category;
import com.hospital.patientadmission.exception.BadRequestException;

/**
 * Normalised query parameters for the list endpoint. Normalising first means "icu" and "ICU"
 * share one cache entry, and unknown filter values fail fast with a clear message.
 */
public record PatientFilter(String name, String ward, String department, String doctor, PatientStatus status) {

    public static PatientFilter of(String name, String ward, String department, String doctor, String status) {
        return new PatientFilter(
                normaliseName(name),
                option(Category.WARD, ward),
                option(Category.DEPARTMENT, department),
                option(Category.DOCTOR, doctor),
                parseStatus(status));
    }

    public String cacheKey() {
        return "n=" + nz(name) + "|w=" + nz(ward) + "|d=" + nz(department)
                + "|doc=" + nz(doctor) + "|s=" + (status == null ? "" : status.name());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String normaliseName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static String option(Category category, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ReferenceData.canonical(category, value).orElseThrow(() -> new BadRequestException(
                "Unknown " + category.name().toLowerCase(Locale.ROOT) + " '" + value.trim()
                        + "'. Allowed values: " + String.join(", ", ReferenceData.options(category))));
    }

    private static PatientStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return PatientStatus.fromLabel(status).orElseThrow(() ->
                new BadRequestException("Unknown status '" + status.trim() + "'. Allowed values: Admitted, Discharged"));
    }
}
