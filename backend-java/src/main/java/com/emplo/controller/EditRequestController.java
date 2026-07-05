package com.emplo.controller;

import com.emplo.entity.EditAccessRequest;
import com.emplo.entity.User;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.EditRequestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/edit-requests")
@RequiredArgsConstructor
public class EditRequestController {

    private final EditRequestService editRequestService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<EditAccessRequest> create(@RequestBody CreateEditRequestDto request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(editRequestService.createRequest(user, request.getSection(), request.getReason()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EditAccessRequest>> myRequests() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(editRequestService.myRequests(user));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<EditAccessRequest>> pendingRequests() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(editRequestService.pendingRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<EditAccessRequest> approve(@PathVariable UUID id, @RequestBody ApproveDto request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(editRequestService.approveRequest(user, id,
                request.getWindowHours() != null ? request.getWindowHours() : 24, request.getRemarks()));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<EditAccessRequest> reject(@PathVariable UUID id, @RequestBody ConfirmDto request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(editRequestService.rejectRequest(user, id, request.getRemarks()));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<EditAccessRequest> submit(@PathVariable UUID id, @RequestBody SubmitDto request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(editRequestService.submitChanges(user, id, request.getData()));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<EditAccessRequest> confirm(@PathVariable UUID id, @RequestBody ConfirmDto request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(editRequestService.confirmChanges(user, id, request.getAction(), request.getRemarks()));
    }

    @Data
    public static class CreateEditRequestDto {
        private EditableSection section;
        private String reason;
    }

    @Data
    public static class ApproveDto {
        private Integer windowHours;
        private String remarks;
    }

    @Data
    public static class SubmitDto {
        private Map<String, Object> data;
    }

    @Data
    public static class ConfirmDto {
        private String action;
        private String remarks;
    }
}
