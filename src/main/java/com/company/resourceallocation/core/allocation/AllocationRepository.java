package com.company.resourceallocation.core.allocation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    @Query("SELECT COALESCE(SUM(a.allocationPercent), 0) FROM Allocation a WHERE a.employee.employeeId = :employeeId AND a.allocationId <> :excludeId")
    Integer sumAllocationByEmployeeExcluding(@Param("employeeId") Long employeeId, @Param("excludeId") Long excludeId);

    @Query("SELECT a FROM Allocation a WHERE " +
           "(:employeeId IS NULL OR a.employee.employeeId = :employeeId) AND " +
           "(:projectId IS NULL OR a.project.projectId = :projectId)")
    Page<Allocation> findFiltered(@Param("employeeId") Long employeeId, 
                                  @Param("projectId") Long projectId, 
                                  Pageable pageable);

    boolean existsByEmployeeEmployeeId(Long employeeId);

    boolean existsByProjectProjectId(Long projectId);
}
