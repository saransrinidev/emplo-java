package com.emplo.controller;

import com.emplo.entity.Certification;
import com.emplo.entity.User;
import com.emplo.entity.enums.CertificationCategory;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.CertificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Certification>> list(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(certificationService.listCertifications(user, employeeId));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<Certification>> expiring(
            @RequestParam(value = "days", defaultValue = "90") int days) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(certificationService.getExpiring(days));
    }

    @PostMapping
    public ResponseEntity<Certification> add(@RequestBody AddCertificationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Certification cert = certificationService.addCertification(user, request.getEmployeeId(),
                request.getCertificateName(), request.getCertificateNumber(),
                request.getCategory(), request.getIssuedDate(), request.getExpiryDate(), request.getFileUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Certification> update(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(certificationService.updateCertification(user, id, updates));
    }

    @Data
    public static class AddCertificationRequest {
        private UUID employeeId;
        private String certificateName;
        private String certificateNumber;
        private CertificationCategory category;
        private LocalDate issuedDate;
        private LocalDate expiryDate;
        private String fileUrl;
    }
}
