package com.emplo.repository;

import com.emplo.entity.LeaveRequest;
import com.emplo.entity.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findAllByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<LeaveRequest> findAllByStatusOrderByCreatedAtDesc(LeaveStatus status);

    List<LeaveRequest> findAllByStatusInOrderByCreatedAtDesc(List<LeaveStatus> statuses);

    List<LeaveRequest> findAllByEmployeeIdInAndStatusOrderByCreatedAtDesc(List<UUID> employeeIds, LeaveStatus status);

    List<LeaveRequest> findAllByEmployeeIdInAndStatusInOrderByCreatedAtDesc(List<UUID> employeeIds, List<LeaveStatus> statuses);

    /**
     * Find overlapping leave requests for an employee that are still active
     * (pending, forwarded_to_hr, or approved) and overlap the given date range.
     * Two ranges overlap when: existing.start <= new.end AND existing.end >= new.start.
     * Optionally excludes a specific request id (useful when editing/resubmitting).
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
            "AND lr.status IN :activeStatuses " +
            "AND lr.startDate <= :endDate AND lr.endDate >= :startDate " +
            "AND (:excludeId IS NULL OR lr.id <> :excludeId)")
    List<LeaveRequest> findOverlapping(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("activeStatuses") List<LeaveStatus> activeStatuses,
            @Param("excludeId") UUID excludeId);
}
