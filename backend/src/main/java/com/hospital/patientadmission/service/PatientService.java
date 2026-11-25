package com.hospital.patientadmission.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.patientadmission.domain.Patient;
import com.hospital.patientadmission.domain.PatientStatus;
import com.hospital.patientadmission.domain.ReferenceData;
import com.hospital.patientadmission.domain.ReferenceData.Category;
import com.hospital.patientadmission.dto.PatientFilter;
import com.hospital.patientadmission.dto.PatientRequest;
import com.hospital.patientadmission.dto.PatientResponse;
import com.hospital.patientadmission.exception.BadRequestException;
import com.hospital.patientadmission.exception.ConflictException;
import com.hospital.patientadmission.exception.PatientNotFoundException;
import com.hospital.patientadmission.repository.PatientRepository;
import com.hospital.patientadmission.repository.PatientSpecifications;

/**
 * Caches: "patients" holds list/search/filter results (one entry per distinct filter combination),
 * "patient" holds single patients by id. Any write evicts the list cache (a change can affect any
 * filter) and the affected single entry.
 */
@Service
public class PatientService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "admissionDate")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @CacheEvict(cacheNames = "patients", allEntries = true)
    public PatientResponse create(PatientRequest request) {
        Patient patient = new Patient();
        apply(patient, request);
        return PatientResponse.from(repository.save(patient));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "patients", key = "#filter.cacheKey()")
    public List<PatientResponse> list(PatientFilter filter) {
        List<PatientResponse> result = new ArrayList<>();
        for (Patient patient : repository.findAll(PatientSpecifications.matching(filter), NEWEST_FIRST)) {
            result.add(PatientResponse.from(patient));
        }
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "patient", key = "#id")
    public PatientResponse get(Long id) {
        return PatientResponse.from(find(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "patients", allEntries = true),
            @CacheEvict(cacheNames = "patient", key = "#id")
    })
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = find(id);
        apply(patient, request);
        return PatientResponse.from(repository.save(patient));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "patients", allEntries = true),
            @CacheEvict(cacheNames = "patient", key = "#id")
    })
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new PatientNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "patients", allEntries = true),
            @CacheEvict(cacheNames = "patient", key = "#id")
    })
    public PatientResponse discharge(Long id) {
        Patient patient = find(id);
        if (patient.getStatus() == PatientStatus.DISCHARGED) {
            throw new ConflictException("Patient with id " + id + " is already discharged");
        }
        patient.setStatus(PatientStatus.DISCHARGED);
        return PatientResponse.from(repository.save(patient));
    }

    // ---- helpers --------------------------------------------------------------

    private Patient find(Long id) {
        return repository.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
    }

    private void apply(Patient patient, PatientRequest request) {
        patient.setName(request.name());
        patient.setAge(request.age());
        patient.setGender(canonical(Category.GENDER, request.gender()));
        patient.setWard(canonical(Category.WARD, request.ward()));
        patient.setDepartment(canonical(Category.DEPARTMENT, request.department()));
        patient.setDoctor(canonical(Category.DOCTOR, request.doctor()));
        patient.setAdmissionDate(request.admissionDate());
        if (request.status() != null) {
            patient.setStatus(PatientStatus.fromLabel(request.status()).orElseThrow(() ->
                    new BadRequestException("status must be Admitted or Discharged")));
        }
    }

    /** Stores the canonical spelling ("icu" -> "ICU"). Validation has already passed; this is a safety net. */
    private String canonical(Category category, String value) {
        return ReferenceData.canonical(category, value).orElseThrow(() ->
                new BadRequestException(category.name().toLowerCase() + " must be one of: "
                        + String.join(", ", ReferenceData.options(category))));
    }
}
