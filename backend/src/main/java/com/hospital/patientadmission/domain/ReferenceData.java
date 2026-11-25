package com.hospital.patientadmission.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The fixed "select from list" values. Single source of truth for validation and the frontend dropdowns. */
public final class ReferenceData {

    public enum Category { GENDER, WARD, DEPARTMENT, DOCTOR }

    private static final Map<Category, List<String>> OPTIONS = new EnumMap<>(Category.class);

    static {
        OPTIONS.put(Category.GENDER, List.of("Male", "Female", "Other"));
        OPTIONS.put(Category.WARD, List.of(
                "General Ward", "ICU", "Emergency", "Maternity Ward", "Pediatric Ward", "Emergency"));
        OPTIONS.put(Category.DEPARTMENT, List.of(
                "Cardiology", "Neurology", "Orthopedics", "Pediatrics",
                "General Medicine", "Oncology"));
        OPTIONS.put(Category.DOCTOR, List.of(
                "Dr. Anil Sharma", "Dr. Meera Iyer", "Dr. Rahul Verma", "Dr. Sara Khan", "Dr. John Smith"));
    }

    private ReferenceData() {
    }

    public static List<String> options(Category category) {
        return OPTIONS.get(category);
    }

    /** Case-insensitive match that returns the canonical spelling, e.g. "icu" -> "ICU". */
    public static Optional<String> canonical(Category category, String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return OPTIONS.get(category).stream()
                .filter(option -> option.equalsIgnoreCase(trimmed))
                .findFirst();
    }
}
