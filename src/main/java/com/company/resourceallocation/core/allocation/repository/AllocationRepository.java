package com.company.resourceallocation.core.allocation.repository;
import com.company.resourceallocation.core.allocation.entity.Allocation;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    List<Allocation> findByEmployeeEmployeeId(Long employeeId);

    @Query("SELECT COALESCE(SUM(a.allocationPercent), 0) FROM Allocation a WHERE a.employee.employeeId = :employeeId AND a.allocationId <> :excludeId AND a.status <> com.company.resourceallocation.core.allocation.entity.AllocationStatus.ENDED")
    Integer sumAllocationByEmployeeExcluding(@Param("employeeId") Long employeeId, @Param("excludeId") Long excludeId);

    @Query("SELECT a FROM Allocation a WHERE " +
           "(:employeeId IS NULL OR a.employee.employeeId = :employeeId) AND " +
           "(:projectId IS NULL OR a.project.projectId = :projectId)")
    Page<Allocation> findFiltered(@Param("employeeId") Long employeeId, 
                                  @Param("projectId") Long projectId, 
                                  Pageable pageable);

    boolean existsByEmployeeEmployeeId(Long employeeId);

    boolean existsByProjectProjectId(Long projectId);

    @Query(value = "SELECT e.employee_id, e.full_name, COALESCE(SUM(a.allocation_percent), 0) FROM employee e LEFT JOIN allocation a ON a.employee_id = e.employee_id AND a.status <> 'ENDED' GROUP BY e.employee_id, e.full_name", nativeQuery = true)
    List<Object[]> getUtilizationReport();

    @Query(value = "SELECT e.employee_id, e.full_name, (100 - COALESCE(SUM(a.allocation_percent), 0)) FROM employee e LEFT JOIN allocation a ON a.employee_id = e.employee_id AND a.status <> 'ENDED' GROUP BY e.employee_id, e.full_name HAVING (100 - COALESCE(SUM(a.allocation_percent), 0)) >= :minAvailable", nativeQuery = true)
    List<Object[]> getAvailableReport(@Param("minAvailable") int minAvailable);

    @Query(value = "SELECT e.employee_id, e.full_name, COALESCE(SUM(a.allocation_percent), 0) FROM employee e LEFT JOIN allocation a ON a.employee_id = e.employee_id AND a.status <> 'ENDED' GROUP BY e.employee_id, e.full_name HAVING COALESCE(SUM(a.allocation_percent), 0) > 90", nativeQuery = true)
    List<Object[]> getOverloadedReport();
}
