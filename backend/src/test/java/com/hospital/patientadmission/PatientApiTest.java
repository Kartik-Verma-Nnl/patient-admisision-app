package com.hospital.patientadmission;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.patientadmission.repository.PatientRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=simple"
})
@AutoConfigureMockMvc
class PatientApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository repository;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    private Map<String, Object> patient(String name, int age, String ward, String dept, String doctor) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("age", age);
        m.put("gender", "Male");
        m.put("ward", ward);
        m.put("department", dept);
        m.put("doctor", doctor);
        m.put("admissionDate", LocalDate.now().toString());
        return m;
    }

    private long create(Map<String, Object> body) throws Exception {
        String response = mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void createsPatientWithDefaultStatusAndLocationHeader() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(patient("Ravi Kumar", 45, "icu", "Cardiology", "Dr. Anil Sharma"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status", is("ADMITTED")))
                .andExpect(jsonPath("$.ward", is("ICU"))); // stored in canonical spelling
    }

    @Test
    void rejectsInvalidFieldsWithClearMessages() throws Exception {
        Map<String, Object> bad = patient("John123", 130, null, "Cardiology", "Dr. Anil Sharma");
        bad.put("gender", "Robot");
        bad.put("admissionDate", LocalDate.now().plusDays(1).toString());

        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("name", "age", "gender", "ward", "admissionDate")));
    }

    @Test
    void rejectsWrongTypes() throws Exception {
        String body = """
                {"name":"Asha","age":25.5,"gender":"Female","ward":"ICU","department":"Cardiology",
                 "doctor":"Dr. Anil Sharma","admissionDate":"2026-13-45"}""";
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("age")))
                .andExpect(jsonPath("$.errors[0].message", is("must be a whole number")));

        String badDate = """
                {"name":"Asha","age":25,"gender":"Female","ward":"ICU","department":"Cardiology",
                 "doctor":"Dr. Anil Sharma","admissionDate":"2026-13-45"}""";
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(badDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("admissionDate")))
                .andExpect(jsonPath("$.errors[0].message", is("must be a valid date in yyyy-MM-dd format")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Malformed JSON request body")));
    }

    @Test
    void filtersAndSearches() throws Exception {
        create(patient("Ravi Kumar", 45, "ICU", "Cardiology", "Dr. Anil Sharma"));
        create(patient("Asha Rao", 30, "Maternity", "Gynecology", "Dr. Priya Mehta"));
        create(patient("Ravina Singh", 60, "ICU", "Neurology", "Dr. Anil Sharma"));

        mvc.perform(get("/api/patients")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/patients").param("ward", "icu")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/patients").param("department", "Neurology")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/patients").param("doctor", "Dr. Anil Sharma")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/patients").param("name", "RAVI")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/patients").param("name", "ravi").param("ward", "ICU").param("department", "Cardiology"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/patients").param("name", "%")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/patients").param("ward", "Nowhere")).andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndDischarges() throws Exception {
        long id = create(patient("Ravi Kumar", 45, "ICU", "Cardiology", "Dr. Anil Sharma"));

        mvc.perform(put("/api/patients/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(patient("Ravi K Sharma", 46, "Surgical", "Cardiology", "Dr. Anil Sharma"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Ravi K Sharma")))
                .andExpect(jsonPath("$.age", is(46)));

        mvc.perform(patch("/api/patients/{id}/discharge", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISCHARGED")));
        mvc.perform(patch("/api/patients/{id}/discharge", id)).andExpect(status().isConflict());
        mvc.perform(get("/api/patients/{id}", id)).andExpect(jsonPath("$.status", is("DISCHARGED")));
    }

    @Test
    void deletesPatient() throws Exception {
        long id = create(patient("Ravi Kumar", 45, "ICU", "Cardiology", "Dr. Anil Sharma"));
        mvc.perform(get("/api/patients/{id}", id)).andExpect(status().isOk()); // populates the cache
        mvc.perform(delete("/api/patients/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/patients/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/patients/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/patients/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void listCacheIsEvictedOnEveryWrite() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(jsonPath("$", hasSize(0)));   // cached as empty
        long id = create(patient("Ravi Kumar", 45, "ICU", "Cardiology", "Dr. Anil Sharma"));
        mvc.perform(get("/api/patients")).andExpect(jsonPath("$", hasSize(1)));   // create evicted it

        mvc.perform(get("/api/patients").param("status", "ADMITTED")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(patch("/api/patients/{id}/discharge", id)).andExpect(status().isOk());
        mvc.perform(get("/api/patients").param("status", "ADMITTED")).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/patients/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/patients")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void exposesDropdownOptions() throws Exception {
        mvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wards", hasItems("ICU", "Emergency")))
                .andExpect(jsonPath("$.statuses", hasItems("ADMITTED", "DISCHARGED")));
    }
}
