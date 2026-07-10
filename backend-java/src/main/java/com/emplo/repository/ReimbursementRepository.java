package com.emplo.repository;

import com.emplo.entity.Reimbursement;
import com.emplo.entity.enums.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {

    List<Reimbursement> findAllByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<Reimbursement> findAllByEmployeeIdInOrderByCreatedAtDesc(List<UUID> employeeIds);

    List<Reimbursement> findAllByStatusOrderByCreatedAtDesc(ReimbursementStatus status);

    List<Reimbursement> findAllByStatusInOrderByCreatedAtDesc(List<ReimbursementStatus> statuses);

    @Query("SELECT COUNT(r) FROM Reimbursement r")
    long countAll();
}
