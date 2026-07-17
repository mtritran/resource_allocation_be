package com.company.resourceallocation.core.employee.repository;

import com.company.resourceallocation.core.employee.entity.Employee;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, Long employeeId);

    boolean existsByEmailAndEmployeeIdNot(String email, Long employeeId);

    @Query("SELECT e FROM Employee e WHERE " +
            "(:department IS NULL OR e.department = :department) AND " +
            "(:role IS NULL OR e.role = :role)")
    Page<Employee> findFiltered(@Param("department") String department,
            @Param("role") String role,
            Pageable pageable);

    @Query("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE LOWER(s.skillName) = LOWER(:skillName)")
    List<Employee> findBySkillName(@Param("skillName") String skillName);
}
