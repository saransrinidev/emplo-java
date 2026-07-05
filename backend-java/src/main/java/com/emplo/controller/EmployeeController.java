package com.emplo.controller;

import com.emplo.dto.employee.*;
import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listEmployees(
            @RequestParam(value = "q", required = false) String query) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(employeeService.listEmployees(user, query));
    }

    @GetMapping("/with-roles")
    public ResponseEntity<List<EmployeeWithRoleResponse>> listWithRoles(
            @RequestParam(value = "q", required = false) String query) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(employeeService.listWithRoles(user, query));
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> getNextEmployeeCode() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(Map.of("employee_code", employeeService.generateNextCode()));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(employeeService.getById(user, employeeId));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(user, request));
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<BulkImportResult> bulkImport(
            @Valid @RequestBody BulkImportRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.bulkImport(user, request));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable UUID employeeId,
            @RequestBody EmployeeUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(employeeService.update(user, employeeId, request));
    }

    @PostMapping("/{employeeId}/create-login")
    public ResponseEntity<UserAccountResponse> createLogin(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateLoginRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createLogin(user, employeeId, request));
    }

    @PostMapping("/bulk-create-login")
    public ResponseEntity<BulkCreateLoginResult> bulkCreateLogin(
            @Valid @RequestBody BulkCreateLoginRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.bulkCreateLogin(user, request));
    }

    @DeleteMapping("/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        employeeService.delete(user, employeeId);
    }

    @PutMapping("/{employeeId}/change-role")
    public ResponseEntity<UserAccountResponse> changeRole(
            @PathVariable UUID employeeId,
            @Valid @RequestBody ChangeRoleRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(employeeService.changeRole(user, employeeId, request));
    }
}
