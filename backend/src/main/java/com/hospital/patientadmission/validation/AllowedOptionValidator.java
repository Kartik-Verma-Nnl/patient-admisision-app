package com.hospital.patientadmission.validation;

import com.hospital.patientadmission.domain.ReferenceData;
import com.hospital.patientadmission.domain.ReferenceData.Category;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowedOptionValidator implements ConstraintValidator<AllowedOption, String> {

    private Category category;

    @Override
    public void initialize(AllowedOption annotation) {
        this.category = annotation.category();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/blank is reported by @NotBlank, so each problem produces exactly one message
        if (value == null || value.isBlank()) {
            return true;
        }
        if (ReferenceData.canonical(category, value).isPresent()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "must be one of: " + String.join(", ", ReferenceData.options(category)))
                .addConstraintViolation();
        return false;
    }
}
