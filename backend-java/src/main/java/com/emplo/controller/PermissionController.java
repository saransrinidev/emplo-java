package com.emplo.controller;

import com.emplo.entity.EmployeeEditPermission;
import com.emplo.entity.User;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.PermissionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<EmployeeEditPermission> grant(@RequestBody GrantRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        EmployeeEditPermission perm = permissionService.grantPermission(user,
                request.getEmployeeId(), request.getSection(), request.getStartAt(), request.getExpiryAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(perm);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeEditPermission>> list(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(permissionService.listPermissions(user, employeeId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        permissionService.revokePermission(user, id);
    }

    @Data
    public static class GrantRequest {
        private UUID employeeId;
        private EditableSection section;
        private Instant startAt;
        private Instant expiryAt;
    }
}
