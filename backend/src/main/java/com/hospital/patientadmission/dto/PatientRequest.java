package com.hospital.patientadmission.dto;

import java.time.LocalDate;

import com.hospital.patientadmission.domain.ReferenceData.Category;
import com.hospital.patientadmission.validation.AllowedOption;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Body for POST and PUT. {@code status} is optional: defaults to ADMITTED on create, unchanged on update. */
public record PatientRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        @Pattern(regexp = "^(?:\\p{L}+(?: \\p{L}+)*)?$", message = "name must contain letters and spaces only")
        String name,

        @NotNull(message = "age is required")
        @Min(value = 0, message = "age must be between 0 and 120")
        @Max(value = 120, message = "age must be between 0 and 120")
        Integer age,

        @NotBlank(message = "gender is required")
        @AllowedOption(category = Category.GENDER)
        String gender,

        @NotBlank(message = "ward is required")
        @AllowedOption(category = Category.WARD)
        String ward,

        @NotBlank(message = "department is required")
        @AllowedOption(category = Category.DEPARTMENT)
        String department,

        @NotBlank(message = "doctor is required")
        @AllowedOption(category = Category.DOCTOR)
        String doctor,

        @NotNull(message = "admissionDate is required")
        @PastOrPresent(message = "admissionDate cannot be a future date")
        LocalDate admissionDate,

        @Pattern(regexp = "^(?i:admitted|discharged)$", message = "status must be ADMITTED or DISCHARGED")
        String status) {

    public PatientRequest {
        name = trim(name);
        gender = trim(gender);
        ward = trim(ward);
        department = trim(department);
        doctor = trim(doctor);
        status = trim(status);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
