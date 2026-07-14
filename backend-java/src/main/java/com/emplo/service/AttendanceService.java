package com.emplo.service;

import com.emplo.dto.attendance.*;
import com.emplo.entity.*;
import com.emplo.entity.enums.LeaveStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeEntityRepository leaveTypeEntityRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final WorkingDaysCalculator workingDaysCalculator;

    // Statuses considered "active" for overlap detection
    private static final List<LeaveStatus> ACTIVE_STATUSES =
            List.of(LeaveStatus.pending, LeaveStatus.forwarded_to_hr, LeaveStatus.approved);

    // ─── Employee: apply for leave ────────────────────────────────────────────

    @Transactional
    public LeaveRequestResponse applyForLeave(User user, LeaveRequestCreateDto dto) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot apply for leave in the past");
        }

        // FIX #2: Prevent duplicate / overlapping requests
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlapping(
                user.getEmployeeId(), dto.getStartDate(), dto.getEndDate(), ACTIVE_STATUSES, null);
        if (!overlaps.isEmpty()) {
            throw new BadRequestException(
                    "You already have a leave request that overlaps these dates. Please check your existing requests.");
        }

        // FIX #4: Count only working days (exclude weekends + holidays)
        double days = workingDaysCalculator.calculateWorkingDays(dto.getStartDate(), dto.getEndDate());
        if (days <= 0) {
            throw new BadRequestException("The selected dates contain no working days (only weekends/holidays).");
        }

        // FIX #3: Validate leave balance before submission
        validateSufficientBalance(user.getEmployeeId(), dto.getLeaveType().name(), days);

        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LeaveRequest lr = LeaveRequest.builder()
                .employeeId(user.getEmployeeId())
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .build();

        // FIX #1: If employee has no manager, auto-forward directly to HR
        if (emp.getManagerId() == null) {
            lr.setStatus(LeaveStatus.forwarded_to_hr);
        } else {
            lr.setStatus(LeaveStatus.pending);
        }

        lr = leaveRequestRepository.save(lr);

        // Reserve balance as pending immediately (whether pending or auto-forwarded)
        updatePendingBalance(lr, days);

        auditService.logAction(user.getId(), "apply_leave", "leave_request",
                lr.getId().toString(), Map.of(
                        "leave_type", lr.getLeaveType().name(),
                        "start_date", lr.getStartDate().toString(),
                        "end_date", lr.getEndDate().toString(),
                        "working_days", String.valueOf(days)));

        String empName = emp.getFullName();
        if (emp.getManagerId() == null) {
            notificationService.notifyHrOnly(user, "Leave Request (Direct to HR)",
                    empName + " applied for " + lr.getLeaveType().name() + " leave from "
                            + lr.getStartDate() + " to " + lr.getEndDate() + " (no manager — needs HR action)");
        } else {
            notificationService.notifyHrAndManager(user, "Leave Request Submitted",
                    empName + " applied for " + lr.getLeaveType().name() + " leave from "
                            + lr.getStartDate() + " to " + lr.getEndDate());
        }

        return toResponse(lr);
    }

    // ─── Employee: revise & resubmit a rejected request (FIX #7) ──────────────

    @Transactional
    public LeaveRequestResponse resubmitLeave(User user, UUID leaveId, LeaveRequestCreateDto dto) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        if (!lr.getEmployeeId().equals(user.getEmployeeId())) {
            throw new ForbiddenException("You can only revise your own leave requests");
        }
        if (lr.getStatus() != LeaveStatus.rejected) {
            throw new BadRequestException("Only rejected requests can be revised and resubmitted");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot apply for leave in the past");
        }

        // Overlap check excluding this request itself
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlapping(
                user.getEmployeeId(), dto.getStartDate(), dto.getEndDate(), ACTIVE_STATUSES, lr.getId());
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("The revised dates overlap another active leave request.");
        }

        double days = workingDaysCalculator.calculateWorkingDays(dto.getStartDate(), dto.getEndDate());
        if (days <= 0) {
            throw new BadRequestException("The selected dates contain no working days.");
        }
        validateSufficientBalance(user.getEmployeeId(), dto.getLeaveType().name(), days);

        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        // Preserve history: keep manager/hr remarks from the prior rejection in the audit,
        // but reset the workflow state for a fresh review cycle.
        lr.setLeaveType(dto.getLeaveType());
        lr.setStartDate(dto.getStartDate());
        lr.setEndDate(dto.getEndDate());
        lr.setReason(dto.getReason());
        lr.setStatus(emp.getManagerId() == null ? LeaveStatus.forwarded_to_hr : LeaveStatus.pending);
        // Clear the previous decision so it can be reviewed again
        lr.setManagerRemarks(null);
        lr.setHrRemarks(null);
        lr.setHrId(null);
        lr = leaveRequestRepository.save(lr);

        updatePendingBalance(lr, days);

        auditService.logAction(user.getId(), "resubmit_leave", "leave_request",
                lr.getId().toString(), Map.of(
                        "leave_type", lr.getLeaveType().name(),
                        "start_date", lr.getStartDate().toString(),
                        "end_date", lr.getEndDate().toString()));

        if (emp.getManagerId() == null) {
            notificationService.notifyHrOnly(user, "Leave Request Revised (Direct to HR)",
                    emp.getFullName() + " revised and resubmitted a leave request.");
        } else {
            notificationService.notifyHrAndManager(user, "Leave Request Revised",
                    emp.getFullName() + " revised and resubmitted a leave request.");
        }

        return toResponse(lr);
    }

    // ─── Lists ─────────────────────────────────────────────────────────────────

    public List<LeaveRequestResponse> myLeaveRequests(User user) {
        if (user.getEmployeeId() == null) return List.of();
        List<LeaveRequest> list = leaveRequestRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId());
        return toResponseList(list);
    }

    public List<LeaveRequestResponse> teamLeaveRequests(User user) {
        if (user.getRole().getName() == RoleName.hr_admin) {
            // HR sees everything awaiting their action
            List<LeaveRequest> list = leaveRequestRepository
                    .findAllByStatusOrderByCreatedAtDesc(LeaveStatus.forwarded_to_hr);
            return toResponseList(list);
        }
        if (user.getEmployeeId() == null) return List.of();
        List<UUID> reportIds = employeeRepository.findAllByManagerId(user.getEmployeeId())
                .stream().map(Employee::getId).toList();
        if (reportIds.isEmpty()) return List.of();
        List<LeaveRequest> list = leaveRequestRepository
                .findAllByEmployeeIdInAndStatusOrderByCreatedAtDesc(reportIds, LeaveStatus.pending);
        return toResponseList(list);
    }

    public List<LeaveRequestResponse> allLeaveRequests(String status) {
        List<LeaveRequest> list;
        if (status != null && !status.isEmpty()) {
            LeaveStatus ls = LeaveStatus.valueOf(status);
            list = leaveRequestRepository.findAllByStatusOrderByCreatedAtDesc(ls);
        } else {
            list = leaveRequestRepository.findAll().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }
        return toResponseList(list);
    }

    // ─── Manager action ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveRequestResponse managerAction(User user, UUID leaveId, LeaveManagerActionDto dto) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
        if (lr.getStatus() != LeaveStatus.pending) {
            throw new BadRequestException("Request is not in pending state");
        }
        if (user.getEmployeeId() == null) {
            throw new ForbiddenException("No employee record linked");
        }
        Employee emp = employeeRepository.findById(lr.getEmployeeId()).orElse(null);
        if (emp == null || !user.getEmployeeId().equals(emp.getManagerId())) {
            throw new ForbiddenException("You are not the manager of this employee");
        }

        double days = workingDaysCalculator.calculateWorkingDays(lr.getStartDate(), lr.getEndDate());

        if ("forward".equals(dto.getAction())) {
            lr.setStatus(LeaveStatus.forwarded_to_hr);
            // pending balance already reserved at apply time; nothing to change
        } else {
            lr.setStatus(LeaveStatus.rejected);
            releasePendingBalance(lr, days);
        }
        lr.setManagerId(user.getEmployeeId());
        lr.setManagerRemarks(dto.getRemarks());
        leaveRequestRepository.save(lr);

        auditService.logAction(user.getId(), "manager_" + dto.getAction() + "_leave", "leave_request",
                lr.getId().toString(), Map.of("status", lr.getStatus().name(),
                        "remarks", dto.getRemarks() != null ? dto.getRemarks() : ""));

        if ("forward".equals(dto.getAction())) {
            notificationService.notifyHrOnly(user, "Leave Forwarded for Approval",
                    "Manager forwarded leave request from " + emp.getFullName() + " (" + lr.getLeaveType().name() + " leave)");
        } else {
            notificationService.notifyEmployeeById(lr.getEmployeeId(), "Leave Request Rejected",
                    "Your " + lr.getLeaveType().name() + " leave request was rejected by your manager."
                            + (dto.getRemarks() != null ? " Remarks: " + dto.getRemarks() : ""));
        }

        return toResponse(lr);
    }

    // ─── HR action ────────────────────────────────────────────────────────────

    @Transactional
    public LeaveRequestResponse hrAction(User user, UUID leaveId, LeaveHRActionDto dto) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
        if (lr.getStatus() != LeaveStatus.forwarded_to_hr) {
            throw new BadRequestException("Request must be forwarded (or auto-forwarded) before HR can act");
        }

        double days = workingDaysCalculator.calculateWorkingDays(lr.getStartDate(), lr.getEndDate());

        if ("approve".equals(dto.getAction())) {
            lr.setStatus(LeaveStatus.approved);
            // FIX #5: ensure balance record exists, move pending -> used
            confirmBalanceOnApprove(lr, days);
        } else {
            lr.setStatus(LeaveStatus.rejected);
            releasePendingBalance(lr, days);
        }
        lr.setHrId(user.getId());
        lr.setHrRemarks(dto.getRemarks());
        leaveRequestRepository.save(lr);

        auditService.logAction(user.getId(), "hr_" + dto.getAction() + "_leave", "leave_request",
                lr.getId().toString(), Map.of("status", lr.getStatus().name(),
                        "remarks", dto.getRemarks() != null ? dto.getRemarks() : ""));

        notificationService.notifyEmployeeById(lr.getEmployeeId(),
                "approve".equals(dto.getAction()) ? "Leave Approved" : "Leave Rejected",
                "Your " + lr.getLeaveType().name() + " leave request has been "
                        + ("approve".equals(dto.getAction()) ? "approved" : "rejected") + " by HR."
                        + (dto.getRemarks() != null ? " Remarks: " + dto.getRemarks() : ""));

        return toResponse(lr);
    }

    // ─── Balance helpers ────────────────────────────────────────────────────────

    /** FIX #3: throw if the employee doesn't have enough remaining balance. */
    private void validateSufficientBalance(UUID employeeId, String leaveTypeCode, double days) {
        LeaveTypeEntity lt = leaveTypeEntityRepository.findByCode(leaveTypeCode).orElse(null);
        if (lt == null) {
            // Leave type not configured as a balance-tracked type — allow (e.g. unpaid)
            return;
        }
        // Unpaid leave typically isn't balance-limited
        if (lt.getIsPaid() != null && !lt.getIsPaid()) {
            return;
        }
        int year = LocalDate.now().getYear();
        LeaveBalance bal = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, lt.getId(), year)
                .orElse(null);
        if (bal == null) {
            throw new BadRequestException(
                    "No leave balance allocated for " + lt.getName() + " this year. Please contact HR.");
        }
        BigDecimal available = bal.getAllocated()
                .subtract(bal.getUsed())
                .subtract(bal.getPending());
        if (available.compareTo(BigDecimal.valueOf(days)) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient %s balance. Requested %.1f day(s) but only %.1f available.",
                    lt.getName(), days, available.doubleValue()));
        }
    }

    /** Reserve pending balance when a request is created/resubmitted. */
    private void updatePendingBalance(LeaveRequest lr, double days) {
        LeaveTypeEntity lt = leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).orElse(null);
        if (lt == null) return;
        int year = LocalDate.now().getYear();
        LeaveBalance bal = getOrCreateBalance(lr.getEmployeeId(), lt.getId(), year);
        bal.setPending(bal.getPending().add(BigDecimal.valueOf(days)));
        leaveBalanceRepository.save(bal);
    }

    /** Release reserved pending balance on rejection. */
    private void releasePendingBalance(LeaveRequest lr, double days) {
        LeaveTypeEntity lt = leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).orElse(null);
        if (lt == null) return;
        int year = LocalDate.now().getYear();
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployeeId(), lt.getId(), year)
                .ifPresent(bal -> {
                    bal.setPending(bal.getPending().subtract(BigDecimal.valueOf(days)).max(BigDecimal.ZERO));
                    leaveBalanceRepository.save(bal);
                });
    }

    /** FIX #5: on approval, ensure a balance row exists, move pending -> used. */
    private void confirmBalanceOnApprove(LeaveRequest lr, double days) {
        LeaveTypeEntity lt = leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).orElse(null);
        if (lt == null) return;
        int year = LocalDate.now().getYear();
        LeaveBalance bal = getOrCreateBalance(lr.getEmployeeId(), lt.getId(), year);
        bal.setPending(bal.getPending().subtract(BigDecimal.valueOf(days)).max(BigDecimal.ZERO));
        bal.setUsed(bal.getUsed().add(BigDecimal.valueOf(days)));
        leaveBalanceRepository.save(bal);
    }

    /** FIX #5: never silently skip — create a zero-allocated balance row if missing. */
    private LeaveBalance getOrCreateBalance(UUID employeeId, UUID leaveTypeId, int year) {
        return leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                        .employeeId(employeeId)
                        .leaveTypeId(leaveTypeId)
                        .year(year)
                        .allocated(BigDecimal.ZERO)
                        .used(BigDecimal.ZERO)
                        .pending(BigDecimal.ZERO)
                        .build()));
    }

    // ─── Response mapping (FIX #6: batch-fetch, no N+1) ───────────────────────

    private List<LeaveRequestResponse> toResponseList(List<LeaveRequest> list) {
        if (list.isEmpty()) return List.of();

        // Batch-fetch all involved employees in a single query
        List<UUID> empIds = list.stream().map(LeaveRequest::getEmployeeId).distinct().toList();
        Map<UUID, Employee> empMap = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        return list.stream().map(lr -> {
            Employee emp = empMap.get(lr.getEmployeeId());
            return buildResponse(lr, emp);
        }).toList();
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        Employee emp = employeeRepository.findById(lr.getEmployeeId()).orElse(null);
        return buildResponse(lr, emp);
    }

    private LeaveRequestResponse buildResponse(LeaveRequest lr, Employee emp) {
        double days = workingDaysCalculator.calculateWorkingDays(lr.getStartDate(), lr.getEndDate());
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployeeId())
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .managerId(lr.getManagerId())
                .managerRemarks(lr.getManagerRemarks())
                .hrId(lr.getHrId())
                .hrRemarks(lr.getHrRemarks())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .employeeName(emp != null ? emp.getFullName() : null)
                .department(emp != null ? emp.getDepartment() : null)
                .workingDays(days)
                .build();
    }
}
