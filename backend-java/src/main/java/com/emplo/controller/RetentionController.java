package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.RetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Retention policy management endpoints (HR Admin only).
 */
@RestController
@RequestMapping("/retention")
@RequiredArgsConstructor
public class RetentionController {

    private final RetentionService retentionService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    /**
     * Preview what data would be affected by the retention policy.
     * Does NOT delete anything — safe to call.
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> preview() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(retentionService.previewRetention());
    }

    /**
     * Manually trigger the retention cleanup (same as the scheduled job).
     * Use with caution — this permanently deletes sensitive documents.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runNow() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        retentionService.runRetentionCleanup();
        return ResponseEntity.ok(Map.of("message", "Retention cleanup completed. Check logs for details."));
    }
}
