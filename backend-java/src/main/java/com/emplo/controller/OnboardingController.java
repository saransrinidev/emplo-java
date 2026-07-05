package com.emplo.controller;

import com.emplo.entity.OnboardingTask;
import com.emplo.entity.OnboardingTemplate;
import com.emplo.entity.User;
import com.emplo.entity.enums.OnboardingCategory;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.OnboardingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    // ─── Templates (HR only) ──────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<List<OnboardingTemplate>> listTemplates() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(onboardingService.listAllTemplates());
    }

    @PostMapping("/templates")
    public ResponseEntity<OnboardingTemplate> createTemplate(@RequestBody TemplateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.createTemplate(
                request.getTitle(), request.getDescription(), request.getCategory(),
                request.getSortOrder(), request.getIsRequired(), request.getDueDays(),
                request.getActionType(), request.getActionUrl()));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<OnboardingTemplate> updateTemplate(@PathVariable UUID id, @RequestBody TemplateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(onboardingService.updateTemplate(id,
                request.getTitle(), request.getDescription(), request.getCategory(),
                request.getSortOrder(), request.getIsRequired(), request.getDueDays(),
                request.getActionType(), request.getActionUrl(), request.getIsActive()));
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        onboardingService.deleteTemplate(id);
    }

    // ─── Initialize onboarding for an employee (HR only) ──────────────────────

    @PostMapping("/initialize/{employeeId}")
    public ResponseEntity<List<OnboardingTask>> initializeOnboarding(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.initializeOnboarding(employeeId));
    }

    // ─── Employee: my onboarding checklist ────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<List<OnboardingTask>> myTasks() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(onboardingService.getMyTasks(user));
    }

    @GetMapping("/my/progress")
    public ResponseEntity<Map<String, Object>> myProgress() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(onboardingService.getMyProgress(user));
    }

    // ─── Employee: complete / skip a task ─────────────────────────────────────

    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<OnboardingTask> completeTask(@PathVariable UUID taskId,
                                                       @RequestBody(required = false) CompleteRequest request) {
        User user = currentUserProvider.getCurrentUser();
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(onboardingService.completeTask(user, taskId, notes));
    }

    @PutMapping("/tasks/{taskId}/skip")
    public ResponseEntity<OnboardingTask> skipTask(@PathVariable UUID taskId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(onboardingService.skipTask(user, taskId));
    }

    // ─── HR: view employee onboarding ─────────────────────────────────────────

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<OnboardingTask>> employeeTasks(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin, RoleName.manager);
        return ResponseEntity.ok(onboardingService.getTasksForEmployee(employeeId));
    }

    // ─── HR: onboarding summary dashboard ─────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> summary() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(onboardingService.getOnboardingSummary());
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class TemplateRequest {
        private String title;
        private String description;
        private OnboardingCategory category;
        private Integer sortOrder;
        private Boolean isRequired;
        private Integer dueDays;
        private String actionType;
        private String actionUrl;
        private Boolean isActive;
    }

    @Data
    public static class CompleteRequest {
        private String notes;
    }
}
