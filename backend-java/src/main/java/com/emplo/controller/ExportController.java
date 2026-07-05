package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/employees")
    public ResponseEntity<byte[]> exportEmployees() throws IOException {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        String csv = exportService.exportEmployees();
        return csvResponse(csv, "employees_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/salary-revisions")
    public ResponseEntity<byte[]> exportSalaryRevisions() throws IOException {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        String csv = exportService.exportSalaryRevisions();
        return csvResponse(csv, "salary_revisions_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<byte[]> exportLeaveRequests() throws IOException {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        String csv = exportService.exportLeaveRequests();
        return csvResponse(csv, "leave_requests_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/attendance")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) throws IOException {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        String csv = exportService.exportAttendance(month, year);
        return csvResponse(csv, "attendance_" + LocalDate.now() + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
