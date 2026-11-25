package com.hospital.patientadmission.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hospital.patientadmission.domain.Patient;
import com.hospital.patientadmission.dto.PatientFilter;

import jakarta.persistence.criteria.Predicate;

public final class PatientSpecifications {

    private PatientSpecifications() {
    }

    /** Builds one WHERE clause from whichever filters are present (all combined with AND). */
    public static Specification<Patient> matching(PatientFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.name() != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + escapeLike(filter.name()) + "%", '\\'));
            }
            if (filter.ward() != null) {
                predicates.add(cb.equal(root.get("ward"), filter.ward()));
            }
            if (filter.department() != null) {
                predicates.add(cb.equal(root.get("department"), filter.department()));
            }
            if (filter.doctor() != null) {
                predicates.add(cb.equal(root.get("doctor"), filter.doctor()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Stops a search for "50%" or "a_b" from acting as a SQL wildcard. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
