package com.hospital.patientadmission.dto;

import java.io.Serializable;
import java.time.LocalDate;

import com.hospital.patientadmission.domain.Patient;

public record PatientResponse(
        Long id,
        String name,
        Integer age,
        String gender,
        String ward,
        String department,
        String doctor,
        LocalDate admissionDate,
        String status) implements Serializable {

    private static final long serialVersionUID = 2L;

    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getName(), p.getAge(), p.getGender(), p.getWard(),
                p.getDepartment(), p.getDoctor(), p.getAdmissionDate(), p.getStatus().label());
    }
}