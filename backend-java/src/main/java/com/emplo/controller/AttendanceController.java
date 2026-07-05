package com.emplo.controller;

import com.emplo.dto.attendance.*;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AttendanceService;
import com.emplo.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> applyForLeave(@Valid @RequestBody LeaveRequestCreateDto dto) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.applyForLeave(user, dto));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> myLeaveRequests() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(attendanceService.myLeaveRequests(user));
    }

    @GetMapping("/team")
    public ResponseEntity<List<LeaveRequestResponse>> teamLeaveRequests() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(attendanceService.teamLeaveRequests(user));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestResponse>> allLeaveRequests(
            @RequestParam(value = "status", required = false) String status) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(attendanceService.allLeaveRequests(status));
    }

    @PutMapping("/{id}/manager")
    public ResponseEntity<LeaveRequestResponse> managerAction(
            @PathVariable UUID id, @Valid @RequestBody LeaveManagerActionDto dto) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager);
        return ResponseEntity.ok(attendanceService.managerAction(user, id, dto));
    }

    @PutMapping("/{id}/hr")
    public ResponseEntity<LeaveRequestResponse> hrAction(
            @PathVariable UUID id, @Valid @RequestBody LeaveHRActionDto dto) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(attendanceService.hrAction(user, id, dto));
    }
}
