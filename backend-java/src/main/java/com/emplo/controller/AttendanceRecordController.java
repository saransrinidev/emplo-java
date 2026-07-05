package com.emplo.controller;

import com.emplo.dto.attendancerecord.AttendanceRecordResponse;
import com.emplo.dto.attendancerecord.CheckInRequest;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AttendanceRecordService;
import com.emplo.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance-records")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceRecordResponse> checkIn(@RequestBody(required = false) CheckInRequest request) {
        User user = currentUserProvider.getCurrentUser();
        String source = request != null ? request.getSource() : "web";
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceRecordService.checkIn(user, source));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceRecordResponse> checkOut() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(attendanceRecordService.checkOut(user));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AttendanceRecordResponse>> myAttendance(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(attendanceRecordService.myAttendance(user, month, year));
    }

    @GetMapping("/team")
    public ResponseEntity<List<AttendanceRecordResponse>> teamAttendance(
            @RequestParam(value = "work_date", required = false) LocalDate workDate) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(attendanceRecordService.teamAttendance(user, workDate));
    }
}
