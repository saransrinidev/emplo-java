package com.emplo.repository;

import com.emplo.entity.SalaryRevision;
import com.emplo.entity.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryRevisionRepository extends JpaRepository<SalaryRevision, UUID> {

    List<SalaryRevision> findAllByEmployeeIdOrderByEffectiveDateDesc(UUID employeeId);

    Optional<SalaryRevision> findFirstByEmployeeIdAndApprovalStatusOrderByEffectiveDateDesc(
            UUID employeeId, ApprovalStatus status);

    List<SalaryRevision> findAllByApprovalStatusOrderByEffectiveDateDesc(ApprovalStatus status);

    @Query("SELECT COUNT(s) FROM SalaryRevision s")
    long countAll();
}
