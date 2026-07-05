package com.emplo.dto.attendancerecord;

import com.emplo.entity.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AttendanceRecordResponse {
    private UUID id;
    private UUID employeeId;
    private LocalDate workDate;
    private Instant checkIn;
    private Instant checkOut;
    private BigDecimal workHours;
    private AttendanceStatus status;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}
