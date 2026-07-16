package com.company.resourceallocation.core.allocation.controller;
import com.company.resourceallocation.core.allocation.repository.AllocationRepository;
import com.company.resourceallocation.core.allocation.entity.Allocation;


import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.core.project.entity.Project;
import com.company.resourceallocation.core.project.repository.ProjectRepository;
import com.company.resourceallocation.core.project.entity.ProjectStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AllocationControllerTest {

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

    Employee emp;
    Project prjActive;
    Project prjCompleted;

    @BeforeEach
    void setupData() {
        allocationRepository.deleteAll();
        employeeRepository.deleteAll();
        projectRepository.deleteAll();

        emp = Employee.builder()
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Nguyen Van Alloc")
                .email("alloc@test.com")
                .role("Dev")
                .build();
        emp = employeeRepository.save(emp);

        prjActive = Project.builder()
                .projectCode("PRJ-ACT")
                .projectName("Active Project")
                .status(ProjectStatus.ACTIVE)
                .build();
        prjActive = projectRepository.save(prjActive);

        prjCompleted = Project.builder()
                .projectCode("PRJ-COM")
                .projectName("Completed Project")
                .status(ProjectStatus.COMPLETED)
                .build();
        prjCompleted = projectRepository.save(prjCompleted);
    }

    @Test
    void should_createAllocationSuccessfully_when_totalPercentIsWithin100() throws Exception {
        // Allocate 60%
        AllocationRequest req1 = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjActive.getProjectId())
                .allocationPercent(60)
                .build();

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allocationPercent").value(60));

        // Allocate remaining 40% (total = 100%)
        AllocationRequest req2 = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjActive.getProjectId())
                .allocationPercent(40)
                .build();

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allocationPercent").value(40));
    }

    @Test
    void should_returnBadRequest_when_totalPercentExceeds100() throws Exception {
        // Allocate 60% first
        AllocationRequest req1 = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjActive.getProjectId())
                .allocationPercent(60)
                .build();

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Allocate another 50% (total = 110%) -> should fail
        AllocationRequest req2 = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjActive.getProjectId())
                .allocationPercent(50)
                .build();

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee allocation exceeds 100%"));
    }

    @Test
    void should_returnBadRequest_when_allocatingToCompletedProject() throws Exception {
        AllocationRequest req = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjCompleted.getProjectId())
                .allocationPercent(50)
                .build();

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot allocate to a COMPLETED project"));
    }

    @Test
    void should_updateAllocationSuccessfully_when_percentIsDecreasedAndWithinLimit() throws Exception {
        // 1. Create allocation X = 60%
        Allocation alc = Allocation.builder()
                .employee(emp)
                .project(prjActive)
                .allocationPercent(60)
                .build();
        alc = allocationRepository.save(alc);

        // 2. Create allocation Y = 40% (total is now 100%)
        Allocation alcY = Allocation.builder()
                .employee(emp)
                .project(prjActive)
                .allocationPercent(40)
                .build();
        allocationRepository.save(alcY);

        // 3. Update X from 60% -> 50% (total becomes 50 + 40 = 90% <= 100%)
        // This must pass because the check should exclude X's original 60% before adding the new 50%!
        AllocationRequest updateReq = AllocationRequest.builder()
                .employeeId(emp.getEmployeeId())
                .projectId(prjActive.getProjectId())
                .allocationPercent(50)
                .build();

        mockMvc.perform(put("/api/v1/allocations/" + alc.getAllocationId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationPercent").value(50));
    }

    @Test
    void should_returnConflict_when_deletingEmployeeWithActiveAllocations() throws Exception {
        // 1. Create allocation
        Allocation alc = Allocation.builder()
                .employee(emp)
                .project(prjActive)
                .allocationPercent(50)
                .build();
        allocationRepository.save(alc);

        // 2. Attempt to delete employee -> should return 409 Conflict (BR-EMP-03 / E10)
        mockMvc.perform(delete("/api/v1/employees/" + emp.getEmployeeId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete employee: still has active allocations"));
    }
}
