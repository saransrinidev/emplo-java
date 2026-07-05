package com.emplo.controller;

import com.emplo.entity.SalaryRevision;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.SalaryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/history")
    public ResponseEntity<List<SalaryRevision>> getHistory(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(salaryService.getHistory(user, employeeId));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSalary(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        SalaryRevision latest = salaryService.getCurrentSalary(user, employeeId);
        if (latest == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of(
                "current_salary", latest.getRevisedSalary(),
                "latest_revision_date", latest.getEffectiveDate().toString()
        ));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SalaryRevision>> getPending() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(salaryService.getPendingRevisions());
    }

    @PostMapping("/revisions")
    public ResponseEntity<SalaryRevision> createRevision(@RequestBody CreateRevisionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        SalaryRevision revision = salaryService.createRevision(user, request.getEmployeeId(),
                request.getRevisedSalary(), request.getEffectiveDate(),
                request.getPreviousSalary(), request.getRevisionPercentage(), request.getComments());
        return ResponseEntity.status(HttpStatus.CREATED).body(revision);
    }

    @PutMapping("/revisions/{id}/approve")
    public ResponseEntity<SalaryRevision> approve(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(salaryService.approveRevision(user, id));
    }

    @PutMapping("/revisions/{id}/reject")
    public ResponseEntity<SalaryRevision> reject(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(salaryService.rejectRevision(user, id));
    }

    @Data
    public static class CreateRevisionRequest {
        private UUID employeeId;
        private BigDecimal revisedSalary;
        private String effectiveDate;
        private BigDecimal previousSalary;
        private BigDecimal revisionPercentage;
        private String comments;
    }
}
