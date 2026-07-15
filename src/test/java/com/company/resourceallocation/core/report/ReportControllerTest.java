package com.company.resourceallocation.core.report;

import com.company.resourceallocation.core.allocation.Allocation;
import com.company.resourceallocation.core.allocation.AllocationRepository;
import com.company.resourceallocation.core.employee.Employee;
import com.company.resourceallocation.core.employee.EmployeeRepository;
import com.company.resourceallocation.core.project.Project;
import com.company.resourceallocation.core.project.ProjectRepository;
import com.company.resourceallocation.core.project.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    AllocationRepository allocationRepository;

    Employee empA;
    Employee empB;
    Employee empC;
    Project prj;

    @BeforeEach
    void setupData() {
        allocationRepository.deleteAll();
        employeeRepository.deleteAll();
        projectRepository.deleteAll();

        prj = Project.builder()
                .projectCode("PRJ-1")
                .projectName("Project 1")
                .status(ProjectStatus.ACTIVE)
                .build();
        prj = projectRepository.save(prj);

        empA = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-A")
                .fullName("Employee A")
                .email("a@test.com")
                .role("Dev")
                .build());

        empB = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-B")
                .fullName("Employee B")
                .email("b@test.com")
                .role("Dev")
                .build());

        empC = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-C")
                .fullName("Employee C")
                .email("c@test.com")
                .role("Dev")
                .build());

        // Allocate A = 100%
        allocationRepository.save(Allocation.builder()
                .employee(empA)
                .project(prj)
                .allocationPercent(100)
                .build());

        // Allocate B = 80%
        allocationRepository.save(Allocation.builder()
                .employee(empB)
                .project(prj)
                .allocationPercent(80)
                .build());

        // Allocate C = 40%
        allocationRepository.save(Allocation.builder()
                .employee(empC)
                .project(prj)
                .allocationPercent(40)
                .build());
    }

    @Test
    void should_returnAllEmployeesWithCorrectTotalAllocation_when_getUtilizationReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/utilization")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                // A has 100%
                .andExpect(jsonPath("$[?(@.employeeId == " + empA.getEmployeeId() + ")].totalAllocation").value(100))
                // B has 80%
                .andExpect(jsonPath("$[?(@.employeeId == " + empB.getEmployeeId() + ")].totalAllocation").value(80))
                // C has 40%
                .andExpect(jsonPath("$[?(@.employeeId == " + empC.getEmployeeId() + ")].totalAllocation").value(40));
    }

    @Test
    void should_returnEmployeesWithAvailableCapacityGreaterThanZero_when_getAvailableReportWithoutMinAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/reports/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Only B and C have available > 0
                // B available = 20%
                .andExpect(jsonPath("$[?(@.employeeId == " + empB.getEmployeeId() + ")].available").value(20))
                // C available = 60%
                .andExpect(jsonPath("$[?(@.employeeId == " + empC.getEmployeeId() + ")].available").value(60))
                // A is not in results
                .andExpect(jsonPath("$[?(@.employeeId == " + empA.getEmployeeId() + ")]").doesNotExist());
    }

    @Test
    void should_returnOnlyEmployeesMeetingMinAvailableThreshold_when_getAvailableReportWithMinAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/reports/available?minAvailable=50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // Only C has available = 60 >= 50
                .andExpect(jsonPath("$[0].employeeId").value(empC.getEmployeeId()))
                .andExpect(jsonPath("$[0].available").value(60));
    }

    @Test
    void should_returnOnlyEmployeesOverloadedGreaterThan90Percent_when_getOverloadedReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overloaded")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // Only A has allocation 100 > 90
                .andExpect(jsonPath("$[0].employeeId").value(empA.getEmployeeId()))
                .andExpect(jsonPath("$[0].totalAllocation").value(100));
    }
}
