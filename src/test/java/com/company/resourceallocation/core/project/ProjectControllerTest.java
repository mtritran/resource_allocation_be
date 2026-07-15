package com.company.resourceallocation.core.project;

import com.company.resourceallocation.core.allocation.AllocationRepository;
import com.company.resourceallocation.core.project.dto.ProjectRequest;
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
public class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    AllocationRepository allocationRepository;

    @BeforeEach
    void cleanUp() {
        allocationRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void should_createProjectWithDefaultPlanningStatus_when_statusNotProvided() throws Exception {
        ProjectRequest req = ProjectRequest.builder()
                .projectCode("PRJ-" + UUID.randomUUID().toString().substring(0, 8))
                .projectName("Test Project")
                .customer("Customer A")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .status(null) // should default to PLANNING
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.status").value("PLANNING"));
    }

    @Test
    void should_returnBadRequest_when_endDateIsBeforeStartDate() throws Exception {
        ProjectRequest req = ProjectRequest.builder()
                .projectCode("PRJ-ERR")
                .projectName("Erroneous Project")
                .startDate(LocalDate.of(2025, 6, 30))
                .endDate(LocalDate.of(2025, 1, 1)) // before startDate
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    void should_returnBadRequest_when_statusIsInvalid() throws Exception {
        String reqJson = "{\"projectCode\":\"PRJ-INV\",\"projectName\":\"Inv\",\"status\":\"INVALID_STATUS\"}";

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_updateProjectStatusToCompletedSuccessfully_when_requested() throws Exception {
        Project prj = Project.builder()
                .projectCode("PRJ-COMP")
                .projectName("Completing Project")
                .status(ProjectStatus.ACTIVE)
                .build();
        prj = projectRepository.save(prj);

        ProjectRequest req = ProjectRequest.builder()
                .projectCode("PRJ-COMP")
                .projectName("Completing Project")
                .status(ProjectStatus.COMPLETED)
                .build();

        mockMvc.perform(put("/api/v1/projects/" + prj.getProjectId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void should_deleteProjectSuccessfully_when_noActiveAllocations() throws Exception {
        Project prj = Project.builder()
                .projectCode("PRJ-DEL")
                .projectName("Deleting Project")
                .status(ProjectStatus.PLANNING)
                .build();
        prj = projectRepository.save(prj);

        mockMvc.perform(delete("/api/v1/projects/" + prj.getProjectId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/" + prj.getProjectId()))
                .andExpect(status().isNotFound());
    }
}
