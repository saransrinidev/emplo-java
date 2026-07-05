package com.emplo.repository;

import com.emplo.entity.LeaveRequest;
import com.emplo.entity.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findAllByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<LeaveRequest> findAllByStatusOrderByCreatedAtDesc(LeaveStatus status);

    List<LeaveRequest> findAllByEmployeeIdInAndStatusOrderByCreatedAtDesc(List<UUID> employeeIds, LeaveStatus status);
}
