package com.company.resourceallocation.core.employee;

import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class EmployeeControllerTest {

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void should_createEmployeeSuccessfully_when_inputIsValid() throws Exception {
        EmployeeRequest req = EmployeeRequest.builder()
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Nguyen Van Test")
                .email("test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .role("Developer")
                .department("IT")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").exists())
                .andExpect(jsonPath("$.employeeCode").value(req.getEmployeeCode()))
                .andExpect(jsonPath("$.email").value(req.getEmail()));
    }

    @Test
    void should_returnBadRequest_when_fullNameIsEmpty() throws Exception {
        EmployeeRequest req = EmployeeRequest.builder()
                .employeeCode("EMP100")
                .fullName("")
                .email("test@test.com")
                .role("Developer")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Full name is required")));
    }

    @Test
    void should_returnBadRequest_when_emailIsInvalid() throws Exception {
        EmployeeRequest req = EmployeeRequest.builder()
                .employeeCode("EMP100")
                .fullName("Nguyen Van Test")
                .email("invalid-email")
                .role("Developer")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid email format")));
    }

    @Test
    void should_updateEmployeeSuccessfully_when_validData() throws Exception {
        Employee emp = Employee.builder()
                .employeeCode("EMP200")
                .fullName("Original Name")
                .email("original@test.com")
                .role("Dev")
                .department("IT")
                .build();
        emp = employeeRepository.save(emp);

        EmployeeRequest req = EmployeeRequest.builder()
                .employeeCode("EMP200")
                .fullName("Updated Name")
                .email("original@test.com") // keep email same
                .role("Senior Dev")
                .department("HR")
                .build();

        mockMvc.perform(put("/api/v1/employees/" + emp.getEmployeeId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.department").value("HR"));
    }

    @Test
    void should_deleteEmployeeSuccessfully_when_noActiveAllocations() throws Exception {
        Employee emp = Employee.builder()
                .employeeCode("EMP300")
                .fullName("To Delete")
                .email("delete@test.com")
                .role("Dev")
                .build();
        emp = employeeRepository.save(emp);

        mockMvc.perform(delete("/api/v1/employees/" + emp.getEmployeeId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/employees/" + emp.getEmployeeId()))
                .andExpect(status().isNotFound());
    }
}
