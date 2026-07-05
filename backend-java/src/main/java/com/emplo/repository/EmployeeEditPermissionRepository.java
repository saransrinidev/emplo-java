package com.emplo.repository;

import com.emplo.entity.EmployeeEditPermission;
import com.emplo.entity.enums.EditableSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeEditPermissionRepository extends JpaRepository<EmployeeEditPermission, UUID> {

    List<EmployeeEditPermission> findAllByEmployeeIdOrderByExpiryAtDesc(UUID employeeId);

    Optional<EmployeeEditPermission> findByEmployeeIdAndSectionAndIsRevokedFalseAndStartAtBeforeAndExpiryAtAfter(
            UUID employeeId, EditableSection section, Instant now1, Instant now2);
}
