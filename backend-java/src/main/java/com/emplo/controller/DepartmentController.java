package com.emplo.controller;

import com.emplo.entity.Department;
import com.emplo.entity.Designation;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.DepartmentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Department>> list(
            @RequestParam(value = "active_only", defaultValue = "true") boolean activeOnly) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(departmentService.listDepartments(activeOnly));
    }

    @PostMapping
    public ResponseEntity<Department> create(@RequestBody DepartmentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDepartment(request.getName(), request.getCode(), request.getHeadEmployeeId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable UUID id, @RequestBody DepartmentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(departmentService.updateDepartment(id, request.getName(), request.getCode(), request.getHeadEmployeeId(), request.getIsActive()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        departmentService.deleteDepartment(id);
    }

    @GetMapping("/designations")
    public ResponseEntity<List<Designation>> listDesignations(
            @RequestParam(value = "department_id", required = false) UUID departmentId,
            @RequestParam(value = "active_only", defaultValue = "true") boolean activeOnly) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(departmentService.listDesignations(departmentId, activeOnly));
    }

    @PostMapping("/designations")
    public ResponseEntity<Designation> createDesignation(@RequestBody DesignationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDesignation(request.getTitle(), request.getDepartmentId(), request.getLevel()));
    }

    @PutMapping("/designations/{id}")
    public ResponseEntity<Designation> updateDesignation(@PathVariable UUID id, @RequestBody DesignationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(departmentService.updateDesignation(id, request.getTitle(), request.getDepartmentId(), request.getLevel(), request.getIsActive()));
    }

    @DeleteMapping("/designations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDesignation(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        departmentService.deleteDesignation(id);
    }

    @Data
    public static class DepartmentRequest {
        private String name;
        private String code;
        private UUID headEmployeeId;
        private Boolean isActive;
    }

    @Data
    public static class DesignationRequest {
        private String title;
        private UUID departmentId;
        private Integer level;
        private Boolean isActive;
    }
}
