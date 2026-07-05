package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/employee")
    public ResponseEntity<Map<String, Object>> employeeDashboard() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(dashboardService.employeeDashboard(user));
    }

    @GetMapping("/manager")
    public ResponseEntity<Map<String, Object>> managerDashboard() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(dashboardService.managerDashboard(user));
    }

    @GetMapping("/hr")
    public ResponseEntity<Map<String, Object>> hrDashboard() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(dashboardService.hrDashboard());
    }

    @GetMapping("/missing-documents")
    public ResponseEntity<Map<String, Object>> missingDocuments() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin, RoleName.manager);
        return ResponseEntity.ok(dashboardService.missingDocuments(user));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(dashboardService.analytics());
    }
}
