package com.emplo.service;

import com.emplo.dto.attendancerecord.AttendanceRecordResponse;
import com.emplo.entity.AttendanceRecord;
import com.emplo.entity.Employee;
import com.emplo.entity.User;
import com.emplo.entity.enums.AttendanceStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.repository.AttendanceRecordRepository;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public AttendanceRecordResponse checkIn(User user, String source) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        LocalDate today = LocalDate.now();
        var existing = attendanceRecordRepository.findByEmployeeIdAndWorkDate(user.getEmployeeId(), today);

        if (existing.isPresent()) {
            AttendanceRecord record = existing.get();
            if (record.getCheckIn() != null) {
                throw new BadRequestException("Already checked in today");
            }
            record.setCheckIn(Instant.now());
            record.setSource(source);
            record.setStatus(AttendanceStatus.present);
            return toResponse(attendanceRecordRepository.save(record));
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(user.getEmployeeId())
                .workDate(today)
                .checkIn(Instant.now())
                .status(AttendanceStatus.present)
                .source(source)
                .build();
        return toResponse(attendanceRecordRepository.save(record));
    }

    @Transactional
    public AttendanceRecordResponse checkOut(User user) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(user.getEmployeeId(), today)
                .orElseThrow(() -> new BadRequestException("Must check in before checking out"));

        if (record.getCheckIn() == null) {
            throw new BadRequestException("Must check in before checking out");
        }
        if (record.getCheckOut() != null) {
            throw new BadRequestException("Already checked out today");
        }

        record.setCheckOut(Instant.now());
        Duration duration = Duration.between(record.getCheckIn(), record.getCheckOut());
        double hours = duration.toSeconds() / 3600.0;
        record.setWorkHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));

        if (hours < 4) {
            record.setStatus(AttendanceStatus.half_day);
        }

        return toResponse(attendanceRecordRepository.save(record));
    }

    public List<AttendanceRecordResponse> myAttendance(User user, Integer month, Integer year) {
        if (user.getEmployeeId() == null) return List.of();
        List<AttendanceRecord> records;
        if (month != null && year != null) {
            records = attendanceRecordRepository.findByEmployeeIdAndMonthAndYear(user.getEmployeeId(), month, year);
        } else {
            records = attendanceRecordRepository.findAllByEmployeeIdOrderByWorkDateDesc(user.getEmployeeId());
        }
        return records.stream().map(this::toResponse).toList();
    }

    public List<AttendanceRecordResponse> teamAttendance(User user, LocalDate workDate) {
        LocalDate targetDate = workDate != null ? workDate : LocalDate.now();
        if (user.getRole().getName() == RoleName.hr_admin) {
            return attendanceRecordRepository.findAllByWorkDateOrderByCheckIn(targetDate)
                    .stream().map(this::toResponse).toList();
        }
        if (user.getEmployeeId() == null) return List.of();
        List<UUID> reportIds = employeeRepository.findAllByManagerId(user.getEmployeeId())
                .stream().map(Employee::getId).toList();
        if (reportIds.isEmpty()) return List.of();
        return attendanceRecordRepository.findAllByEmployeeIdInAndWorkDate(reportIds, targetDate)
                .stream().map(this::toResponse).toList();
    }

    private AttendanceRecordResponse toResponse(AttendanceRecord r) {
        return AttendanceRecordResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployeeId())
                .workDate(r.getWorkDate())
                .checkIn(r.getCheckIn())
                .checkOut(r.getCheckOut())
                .workHours(r.getWorkHours())
                .status(r.getStatus())
                .source(r.getSource())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
