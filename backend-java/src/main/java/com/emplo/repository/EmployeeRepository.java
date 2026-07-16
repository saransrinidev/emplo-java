package com.emplo.repository;

import com.emplo.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeCode(String code);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAllByManagerId(UUID managerId);

    List<Employee> findAllByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String code, String email);

    // Active employees only (soft delete filter)
    List<Employee> findAllByIsActiveTrue();

    List<Employee> findAllByIsActiveTrueAndManagerId(UUID managerId);

    List<Employee> findAllByIsActiveFalse(); // terminated employees

    List<Employee> findAllByIsActiveTrueAndFullNameContainingIgnoreCaseOrIsActiveTrueAndEmployeeCodeContainingIgnoreCaseOrIsActiveTrueAndEmailContainingIgnoreCase(
            String name, String code, String email);
}
