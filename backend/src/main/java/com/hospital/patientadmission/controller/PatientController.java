package com.hospital.patientadmission.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hospital.patientadmission.dto.PatientFilter;
import com.hospital.patientadmission.dto.PatientRequest;
import com.hospital.patientadmission.dto.PatientResponse;
import com.hospital.patientadmission.service.PatientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        PatientResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** All filters are optional and combine with AND. {@code name} is a case-insensitive "contains" search. */
    @GetMapping
    public List<PatientResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String doctor,
            @RequestParam(required = false) String status) {
        return service.list(PatientFilter.of(name, ward, department, doctor, status));
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/discharge")
    public PatientResponse discharge(@PathVariable Long id) {
        return service.discharge(id);
    }
}
