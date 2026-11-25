package com.hospital.patientadmission.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.patientadmission.domain.PatientStatus;
import com.hospital.patientadmission.domain.ReferenceData;
import com.hospital.patientadmission.domain.ReferenceData.Category;

/** Lets the React app fill its dropdowns from the same lists the backend validates against. */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/options")
    public Map<String, List<String>> options() {
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("genders", ReferenceData.options(Category.GENDER));
        options.put("wards", ReferenceData.options(Category.WARD));
        options.put("departments", ReferenceData.options(Category.DEPARTMENT));
        options.put("doctors", ReferenceData.options(Category.DOCTOR));
        options.put("statuses", List.of(PatientStatus.ADMITTED.label(), PatientStatus.DISCHARGED.label()));
        return options;
    }
}
