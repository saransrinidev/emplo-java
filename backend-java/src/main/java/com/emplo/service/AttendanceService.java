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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeEntityRepository leaveTypeEntityRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public LeaveRequestResponse applyForLeave(User user, LeaveRequestCreateDto dto) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        LeaveRequest lr = LeaveRequest.builder()
                .employeeId(user.getEmployeeId())
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(LeaveStatus.pending)
                .build();
        lr = leaveRequestRepository.save(lr);

        auditService.logAction(user.getId(), "apply_leave", "leave_request",
                lr.getId().toString(), Map.of(
                        "leave_type", lr.getLeaveType().name(),
                        "start_date", lr.getStartDate().toString(),
                        "end_date", lr.getEndDate().toString()));

        Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        String empName = emp != null ? emp.getFullName() : "An employee";
        notificationService.notifyHrAndManager(user, "Leave Request Submitted",
                empName + " applied for " + lr.getLeaveType().name() + " leave from " + lr.getStartDate() + " to " + lr.getEndDate());

        return toResponse(lr);
    }

    public List<LeaveRequestResponse> myLeaveRequests(User user) {
        if (user.getEmployeeId() == null) return List.of();
        return leaveRequestRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId())
                .stream().map(this::toResponse).toList();
    }

    public List<LeaveRequestResponse> teamLeaveRequests(User user) {
        if (user.getRole().getName() == RoleName.hr_admin) {
            return leaveRequestRepository.findAllByStatusOrderByCreatedAtDesc(LeaveStatus.forwarded_to_hr)
                    .stream().map(this::toResponse).toList();
        }
        if (user.getEmployeeId() == null) return List.of();
        List<UUID> reportIds = employeeRepository.findAllByManagerId(user.getEmployeeId())
                .stream().map(Employee::getId).toList();
        if (reportIds.isEmpty()) return List.of();
        return leaveRequestRepository.findAllByEmployeeIdInAndStatusOrderByCreatedAtDesc(reportIds, LeaveStatus.pending)
                .stream().map(this::toResponse).toList();
    }

    public List<LeaveRequestResponse> allLeaveRequests(String status) {
        if (status != null && !status.isEmpty()) {
            LeaveStatus ls = LeaveStatus.valueOf(status);
            return leaveRequestRepository.findAllByStatusOrderByCreatedAtDesc(ls)
                    .stream().map(this::toResponse).toList();
        }
        return leaveRequestRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse).toList();
    }

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

        if ("forward".equals(dto.getAction())) {
            lr.setStatus(LeaveStatus.forwarded_to_hr);
            updateLeaveBalanceOnForward(lr);
        } else {
            lr.setStatus(LeaveStatus.rejected);
        }
        lr.setManagerId(user.getEmployeeId());
        lr.setManagerRemarks(dto.getRemarks());
        leaveRequestRepository.save(lr);

        auditService.logAction(user.getId(), "manager_" + dto.getAction() + "_leave", "leave_request",
                lr.getId().toString(), Map.of("status", lr.getStatus().name(), "remarks", dto.getRemarks() != null ? dto.getRemarks() : ""));

        if ("forward".equals(dto.getAction())) {
            notificationService.notifyHrOnly(user, "Leave Forwarded for Approval",
                    "Manager forwarded leave request from " + (emp.getFullName()) + " (" + lr.getLeaveType().name() + " leave)");
        }

        return toResponse(lr);
    }

    @Transactional
    public LeaveRequestResponse hrAction(User user, UUID leaveId, LeaveHRActionDto dto) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
        if (lr.getStatus() != LeaveStatus.forwarded_to_hr) {
            throw new BadRequestException("Request must be forwarded by manager before HR can act");
        }

        if ("approve".equals(dto.getAction())) {
            lr.setStatus(LeaveStatus.approved);
            updateLeaveBalanceOnApprove(lr);
        } else {
            lr.setStatus(LeaveStatus.rejected);
            updateLeaveBalanceOnReject(lr);
        }
        lr.setHrId(user.getId());
        lr.setHrRemarks(dto.getRemarks());
        leaveRequestRepository.save(lr);

        auditService.logAction(user.getId(), "hr_" + dto.getAction() + "_leave", "leave_request",
                lr.getId().toString(), Map.of("status", lr.getStatus().name(), "remarks", dto.getRemarks() != null ? dto.getRemarks() : ""));

        return toResponse(lr);
    }

    private double calculateDays(LeaveRequest lr) {
        return ChronoUnit.DAYS.between(lr.getStartDate(), lr.getEndDate()) + 1;
    }

    private void updateLeaveBalanceOnForward(LeaveRequest lr) {
        double days = calculateDays(lr);
        leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).ifPresent(lt -> {
            int year = LocalDate.now().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployeeId(), lt.getId(), year)
                    .ifPresent(bal -> {
                        bal.setPending(bal.getPending().add(BigDecimal.valueOf(days)));
                        leaveBalanceRepository.save(bal);
                    });
        });
    }

    private void updateLeaveBalanceOnApprove(LeaveRequest lr) {
        double days = calculateDays(lr);
        leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).ifPresent(lt -> {
            int year = LocalDate.now().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployeeId(), lt.getId(), year)
                    .ifPresent(bal -> {
                        bal.setPending(bal.getPending().subtract(BigDecimal.valueOf(days)).max(BigDecimal.ZERO));
                        bal.setUsed(bal.getUsed().add(BigDecimal.valueOf(days)));
                        leaveBalanceRepository.save(bal);
                    });
        });
    }

    private void updateLeaveBalanceOnReject(LeaveRequest lr) {
        double days = calculateDays(lr);
        leaveTypeEntityRepository.findByCode(lr.getLeaveType().name()).ifPresent(lt -> {
            int year = LocalDate.now().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployeeId(), lt.getId(), year)
                    .ifPresent(bal -> {
                        bal.setPending(bal.getPending().subtract(BigDecimal.valueOf(days)).max(BigDecimal.ZERO));
                        leaveBalanceRepository.save(bal);
                    });
        });
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        Employee emp = employeeRepository.findById(lr.getEmployeeId()).orElse(null);
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
                .build();
    }
}
