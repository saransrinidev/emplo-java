package com.emplo.controller;

import com.emplo.dto.policy.PolicyDtos.CreatePolicyRequest;
import com.emplo.dto.policy.PolicyDtos.UpdatePolicyRequest;
import com.emplo.entity.Policy;
import com.emplo.entity.User;
import com.emplo.entity.enums.PolicyCategory;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    // ─── All: list published policies ─────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Policy>> list(@RequestParam(required = false) PolicyCategory category) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(policyService.listPublished(category));
    }

    // ─── HR: list all (including drafts) ──────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Policy>> listAll() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(policyService.listAll());
    }

    // ─── All: pending acknowledgements for current user ───────────────────────

    @GetMapping("/pending-acknowledgement")
    public ResponseEntity<List<Policy>> pendingAcknowledgements() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(policyService.myPendingAcknowledgements(user));
    }

    // ─── Single policy ─────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Policy> get(@PathVariable UUID id) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(policyService.get(id));
    }

    @GetMapping("/{id}/acknowledged")
    public ResponseEntity<Map<String, Boolean>> hasAcknowledged(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(Map.of("acknowledged", policyService.hasAcknowledged(user, id)));
    }

    // ─── Employee: acknowledge ─────────────────────────────────────────────────

    @PostMapping("/{id}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        policyService.acknowledge(user, id);
    }

    // ─── HR: acknowledgement stats ─────────────────────────────────────────────

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(policyService.acknowledgementStats(id));
    }

    // ─── HR: create/update/delete ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Policy> create(@Valid @RequestBody CreatePolicyRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        Policy policy = policyService.create(user, request.getTitle(), request.getCategory(),
                request.getContent(), request.getAttachmentUrl(), request.getRequiresAcknowledgement(), request.getIsPublished());
        return ResponseEntity.status(HttpStatus.CREATED).body(policy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Policy> update(@PathVariable UUID id, @Valid @RequestBody UpdatePolicyRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        Policy policy = policyService.update(user, id, request.getTitle(), request.getCategory(),
                request.getContent(), request.getAttachmentUrl(), request.getRequiresAcknowledgement(), request.getIsPublished());
        return ResponseEntity.ok(policy);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        policyService.delete(id);
    }
}
