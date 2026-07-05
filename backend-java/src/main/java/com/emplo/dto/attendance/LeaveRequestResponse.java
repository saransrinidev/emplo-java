package com.emplo.dto.attendance;

import com.emplo.entity.enums.LeaveStatus;
import com.emplo.entity.enums.LeaveType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LeaveRequestResponse {
    private UUID id;
    private UUID employeeId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private UUID managerId;
    private String managerRemarks;
    private UUID hrId;
    private String hrRemarks;
    private Instant createdAt;
    private Instant updatedAt;
    private String employeeName;
    private String department;
}
