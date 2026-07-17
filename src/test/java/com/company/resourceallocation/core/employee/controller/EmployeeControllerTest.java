package com.company.resourceallocation.core.employee.controller;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.project.entity.Project;
import com.company.resourceallocation.core.project.repository.ProjectRepository;
import com.company.resourceallocation.core.allocation.entity.Allocation;
import com.company.resourceallocation.core.allocation.repository.AllocationRepository;
import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    AllocationRepository allocationRepository;

    @BeforeEach
    void cleanUp() {
        allocationRepository.deleteAll();
        employeeRepository.deleteAll();
        projectRepository.deleteAll();
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
                .email("original@test.com") 
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

    @Test
    void should_returnEmployeeWorkloadWithBreakdown_when_employeeHasAllocations() throws Exception {
        Employee emp = Employee.builder()
                .employeeCode("EMP_WL_1")
                .fullName("Tuan Ho Anh")
                .email("tuanwl@test.com")
                .role("Dev")
                .build();
        emp = employeeRepository.save(emp);

        Project prj1 = Project.builder()
                .projectCode("NCG")
                .projectName("New Core")
                .status(com.company.resourceallocation.core.project.entity.ProjectStatus.ACTIVE)
                .build();
        prj1 = projectRepository.save(prj1);

        Project prj2 = Project.builder()
                .projectCode("GRID")
                .projectName("Grid System")
                .status(com.company.resourceallocation.core.project.entity.ProjectStatus.ACTIVE)
                .build();
        prj2 = projectRepository.save(prj2);

        Allocation alc1 = Allocation.builder()
                .employee(emp)
                .project(prj1)
                .allocationPercent(60)
                .build();
        allocationRepository.save(alc1);

        Allocation alc2 = Allocation.builder()
                .employee(emp)
                .project(prj2)
                .allocationPercent(20)
                .build();
        allocationRepository.save(alc2);

        mockMvc.perform(get("/api/v1/employees/" + emp.getEmployeeId() + "/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(emp.getEmployeeId()))
                .andExpect(jsonPath("$.allocated").value(80))
                .andExpect(jsonPath("$.available").value(20))
                .andExpect(jsonPath("$.allocations.length()").value(2))
                .andExpect(jsonPath("$.allocations[0].projectCode").value("NCG"))
                .andExpect(jsonPath("$.allocations[0].allocationPercent").value(60));
    }

    @Test
    void should_returnZeroWorkload_when_employeeHasNoAllocations() throws Exception {
        Employee emp = Employee.builder()
                .employeeCode("EMP_WL_2")
                .fullName("Nam")
                .email("namwl@test.com")
                .role("Dev")
                .build();
        emp = employeeRepository.save(emp);

        mockMvc.perform(get("/api/v1/employees/" + emp.getEmployeeId() + "/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(emp.getEmployeeId()))
                .andExpect(jsonPath("$.allocated").value(0))
                .andExpect(jsonPath("$.available").value(100))
                .andExpect(jsonPath("$.allocations.length()").value(0));
    }

    @Test
    void should_assignAndGetSkills_when_valid() throws Exception {
        Employee emp = Employee.builder()
                .employeeCode("EMP_SKILL_1")
                .fullName("Skill Test Employee")
                .email("skilltest@test.com")
                .role("Dev")
                .build();
        emp = employeeRepository.save(emp);

        mockMvc.perform(post("/api/v1/employees/" + emp.getEmployeeId() + "/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"Java\", \"Spring Boot\"]"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/employees/" + emp.getEmployeeId() + "/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.skillName == 'Java')]").exists())
                .andExpect(jsonPath("$[?(@.skillName == 'Spring Boot')]").exists());

        mockMvc.perform(get("/api/v1/employees/search?skill=Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeName").value("Skill Test Employee"))
                .andExpect(jsonPath("$[0].available").value(100));
    }
}
