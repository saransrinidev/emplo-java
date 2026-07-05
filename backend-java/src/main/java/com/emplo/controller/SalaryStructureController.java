package com.emplo.controller;

import com.emplo.entity.SalaryStructure;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.SalaryStructureService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/salary-structure")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/my")
    public ResponseEntity<?> getMy() {
        User user = currentUserProvider.getCurrentUser();
        SalaryStructure s = salaryStructureService.getMySalaryStructure(user);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<?> get(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        SalaryStructure s = salaryStructureService.getSalaryStructure(user, employeeId);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/template/default")
    public ResponseEntity<Map<String, Object>> getDefaultTemplate() {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(salaryStructureService.getDefaultTemplate());
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<SalaryStructure> upsert(@PathVariable UUID employeeId,
                                                   @RequestBody SalaryStructureRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        SalaryStructure s = salaryStructureService.upsert(employeeId, request.getEarnings(),
                request.getDeductions(), request.getEmployerContributions(), request.getEffectiveDate());
        return ResponseEntity.ok(s);
    }

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, BigDecimal>> calculate(@RequestBody SalaryStructureRequest request) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(salaryStructureService.calculateTotals(
                request.getEarnings(), request.getDeductions(), request.getEmployerContributions()));
    }

    @Data
    public static class SalaryStructureRequest {
        private Map<String, Object> earnings;
        private Map<String, Object> deductions;
        private Map<String, Object> employerContributions;
        private String effectiveDate;
    }
}
