package com.emplo.controller;

import com.emplo.entity.ProfileChangeRequest;
import com.emplo.entity.User;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.ProfileChangeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile-changes")
@RequiredArgsConstructor
public class ProfileChangeController {

    private final ProfileChangeService profileChangeService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<ProfileChangeRequest> submit(@RequestBody SubmitChangeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileChangeService.submitChange(user, request.getSection(), request.getProposedChanges()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProfileChangeRequest>> myChanges() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileChangeService.myChanges(user));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ProfileChangeRequest>> pending() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(profileChangeService.pendingChanges());
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ProfileChangeRequest> review(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(profileChangeService.reviewChange(user, id, request.getAction(), request.getRemarks()));
    }

    @Data
    public static class SubmitChangeRequest {
        private EditableSection section;
        private Map<String, Object> proposedChanges;
    }

    @Data
    public static class ReviewRequest {
        private String action;
        private String remarks;
    }
}
