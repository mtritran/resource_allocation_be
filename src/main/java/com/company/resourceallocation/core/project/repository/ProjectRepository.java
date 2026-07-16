package com.company.resourceallocation.core.project.repository;
import com.company.resourceallocation.core.project.entity.Project;
import com.company.resourceallocation.core.project.entity.ProjectStatus;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByProjectCode(String projectCode);

    boolean existsByProjectCodeAndProjectIdNot(String projectCode, Long projectId);

    @Query("SELECT p FROM Project p WHERE " +
           "(:status IS NULL OR p.status = :status)")
    Page<Project> findFiltered(@Param("status") ProjectStatus status, Pageable pageable);
}
