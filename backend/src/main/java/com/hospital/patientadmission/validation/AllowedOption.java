package com.hospital.patientadmission.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.hospital.patientadmission.domain.ReferenceData.Category;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The value must be one of the predefined options for the given category (case-insensitive). */
@Documented
@Constraint(validatedBy = AllowedOptionValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedOption {

    Category category();

    String message() default "must be one of the allowed values";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
