package com.emplo.controller;

import com.emplo.dto.reimbursement.ReimbursementDtos.ActionRequest;
import com.emplo.dto.reimbursement.ReimbursementDtos.SubmitClaimRequest;
import com.emplo.entity.Reimbursement;
import com.emplo.entity.User;
import com.emplo.entity.enums.ReimbursementStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.ReimbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Reimbursements")
@RestController
@RequestMapping("/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    // ─── Employee: submit a claim ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Reimbursement> submit(@Valid @RequestBody SubmitClaimRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Reimbursement claim = reimbursementService.submitClaim(
                user, request.getClaimantName(), request.getCategory(), request.getTitle(),
                request.getDescription(), request.getAmount(), request.getExpenseDate(), request.getBillUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    // ─── Employee: my claims ───────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<List<Reimbursement>> myClaims() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(reimbursementService.myClaims(user));
    }

    // ─── Manager/HR: team claims awaiting assurance ────────────────────────────

    @GetMapping("/team")
    public ResponseEntity<List<Reimbursement>> teamClaims() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.teamClaims(user));
    }

    // ─── HR: claims ready for final review ─────────────────────────────────────

    @GetMapping("/pending-hr")
    public ResponseEntity<List<Reimbursement>> pendingHr() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.pendingHrReview());
    }

    // ─── HR: list all claims (optionally filtered by status) ──────────────────

    @GetMapping
    public ResponseEntity<List<Reimbursement>> listAll(@RequestParam(required = false) ReimbursementStatus status) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.listAll(status));
    }

    // ─── Single claim ──────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Reimbursement> getClaim(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(reimbursementService.getClaim(user, id));
    }

    // ─── Manager: assurance approve/reject ─────────────────────────────────────

    @PutMapping("/{id}/manager-approve")
    public ResponseEntity<Reimbursement> managerApprove(@PathVariable UUID id, @RequestBody(required = false) ActionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager);
        return ResponseEntity.ok(reimbursementService.managerAction(user, id, true, request != null ? request.getRemarks() : null));
    }

    @PutMapping("/{id}/manager-reject")
    public ResponseEntity<Reimbursement> managerReject(@PathVariable UUID id, @RequestBody(required = false) ActionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager);
        return ResponseEntity.ok(reimbursementService.managerAction(user, id, false, request != null ? request.getRemarks() : null));
    }

    // ─── HR: final approve/reject ───────────────────────────────────────────────

    @PutMapping("/{id}/hr-approve")
    public ResponseEntity<Reimbursement> hrApprove(@PathVariable UUID id, @RequestBody(required = false) ActionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.hrAction(user, id, true, request != null ? request.getRemarks() : null));
    }

    @PutMapping("/{id}/hr-reject")
    public ResponseEntity<Reimbursement> hrReject(@PathVariable UUID id, @RequestBody(required = false) ActionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.hrAction(user, id, false, request != null ? request.getRemarks() : null));
    }

    // ─── HR: mark as paid ────────────────────────────────────────────────────────

    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<Reimbursement> markPaid(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(reimbursementService.markPaid(user, id));
    }
}
