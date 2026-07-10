package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.Reimbursement;
import com.emplo.entity.User;
import com.emplo.entity.enums.ReimbursementCategory;
import com.emplo.entity.enums.ReimbursementStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.ReimbursementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reimbursement claim workflow:
 * 1. Employee submits (claimant name, category, amount, bill, description) -> pending
 * 2. Manager reviews as an "assurance" check -> manager_approved / manager_rejected
 * 3. Only if manager approved: HR makes the final call -> hr_approved / hr_rejected
 * 4. HR marks paid once processed -> paid
 */
@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private final ReimbursementRepository reimbursementRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ─── Employee: submit a claim ─────────────────────────────────────────────

    @Transactional
    public Reimbursement submitClaim(User user, String claimantName, ReimbursementCategory category,
                                     String title, String description, BigDecimal amount,
                                     LocalDate expenseDate, String billUrl) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }

        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        String resolvedName = (claimantName != null && !claimantName.isBlank())
                ? claimantName.trim() : emp.getFullName();

        long count = reimbursementRepository.countAll();
        String claimNumber = String.format("RB-%04d", count + 1);

        Reimbursement claim = Reimbursement.builder()
                .claimNumber(claimNumber)
                .employeeId(emp.getId())
                .claimantName(resolvedName)
                .category(category != null ? category : ReimbursementCategory.other)
                .title(title.trim())
                .description(description)
                .amount(amount)
                .expenseDate(expenseDate)
                .billUrl(billUrl)
                .status(ReimbursementStatus.pending)
                .managerId(emp.getManagerId())
                .build();

        claim = reimbursementRepository.save(claim);

        auditService.logAction(user.getId(), "submit_reimbursement", "reimbursement",
                claim.getId().toString(), java.util.Map.of(
                        "claim_number", claimNumber, "amount", amount.toString(), "category", claim.getCategory().name()));

        // Notify manager (if assigned) and HR
        notificationService.notifyHrAndManager(user, "New Reimbursement Claim: " + claimNumber,
                resolvedName + " submitted a " + claim.getCategory().name().replace("_", " ")
                        + " expense claim for \u20b9" + amount + " (" + title.trim() + ")");

        return claim;
    }

    // ─── Employee: view my claims ──────────────────────────────────────────────

    public List<Reimbursement> myClaims(User user) {
        if (user.getEmployeeId() == null) return List.of();
        return reimbursementRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId());
    }

    // ─── Manager: view team claims awaiting assurance ─────────────────────────

    public List<Reimbursement> teamClaims(User user) {
        if (user.getRole().getName() == RoleName.hr_admin) {
            return reimbursementRepository.findAllByStatusOrderByCreatedAtDesc(ReimbursementStatus.pending);
        }
        if (user.getEmployeeId() == null) return List.of();
        List<UUID> reportIds = employeeRepository.findAllByManagerId(user.getEmployeeId())
                .stream().map(Employee::getId).toList();
        if (reportIds.isEmpty()) return List.of();
        return reimbursementRepository.findAllByEmployeeIdInOrderByCreatedAtDesc(reportIds);
    }

    // ─── HR: view claims awaiting final decision ──────────────────────────────

    public List<Reimbursement> pendingHrReview() {
        return reimbursementRepository.findAllByStatusOrderByCreatedAtDesc(ReimbursementStatus.manager_approved);
    }

    // ─── HR: view all claims ───────────────────────────────────────────────────

    public List<Reimbursement> listAll(ReimbursementStatus status) {
        if (status != null) {
            return reimbursementRepository.findAllByStatusOrderByCreatedAtDesc(status);
        }
        return reimbursementRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
    }

    // ─── Single claim (role-checked) ──────────────────────────────────────────

    public Reimbursement getClaim(User user, UUID claimId) {
        Reimbursement claim = reimbursementRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Reimbursement claim not found"));

        RoleName role = user.getRole().getName();
        if (role == RoleName.hr_admin) return claim;
        if (role == RoleName.employee && !claim.getEmployeeId().equals(user.getEmployeeId())) {
            throw new ForbiddenException("Not authorized to view this claim");
        }
        if (role == RoleName.manager) {
            boolean isOwn = claim.getEmployeeId().equals(user.getEmployeeId());
            boolean isDirectReport = user.getEmployeeId() != null && user.getEmployeeId().equals(claim.getManagerId());
            if (!isOwn && !isDirectReport) {
                throw new ForbiddenException("Not authorized to view this claim");
            }
        }
        return claim;
    }

    // ─── Manager: give assurance (approve/reject) ─────────────────────────────

    @Transactional
    public Reimbursement managerAction(User user, UUID claimId, boolean approve, String remarks) {
        Reimbursement claim = reimbursementRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Reimbursement claim not found"));

        if (claim.getStatus() != ReimbursementStatus.pending) {
            throw new BadRequestException("This claim has already been reviewed by a manager");
        }
        if (user.getEmployeeId() == null || !user.getEmployeeId().equals(claim.getManagerId())) {
            throw new ForbiddenException("You are not the manager for this employee's claim");
        }

        claim.setStatus(approve ? ReimbursementStatus.manager_approved : ReimbursementStatus.manager_rejected);
        claim.setManagerRemarks(remarks);
        claim.setManagerActedAt(Instant.now());
        claim = reimbursementRepository.save(claim);

        auditService.logAction(user.getId(), approve ? "manager_approve_reimbursement" : "manager_reject_reimbursement",
                "reimbursement", claim.getId().toString(),
                java.util.Map.of("status", claim.getStatus().name()));

        if (approve) {
            // Notify HR that it's ready for final review
            notificationService.notifyHrOnly(user, "Reimbursement Ready for Final Review: " + claim.getClaimNumber(),
                    "Manager approved " + claim.getClaimantName() + "'s claim of \u20b9" + claim.getAmount()
                            + ". Awaiting HR final decision.");
        } else {
            // Notify employee of rejection
            notifyEmployee(claim, "Reimbursement Rejected",
                    "Your claim " + claim.getClaimNumber() + " was rejected by your manager. "
                            + (remarks != null ? remarks : ""));
        }

        return claim;
    }

    // ─── HR: final approve/reject ─────────────────────────────────────────────

    @Transactional
    public Reimbursement hrAction(User user, UUID claimId, boolean approve, String remarks) {
        Reimbursement claim = reimbursementRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Reimbursement claim not found"));

        if (claim.getStatus() != ReimbursementStatus.manager_approved) {
            throw new BadRequestException("Claim must be approved by manager before HR can act");
        }

        claim.setStatus(approve ? ReimbursementStatus.hr_approved : ReimbursementStatus.hr_rejected);
        claim.setHrId(user.getId());
        claim.setHrRemarks(remarks);
        claim.setHrActedAt(Instant.now());
        claim = reimbursementRepository.save(claim);

        auditService.logAction(user.getId(), approve ? "hr_approve_reimbursement" : "hr_reject_reimbursement",
                "reimbursement", claim.getId().toString(),
                java.util.Map.of("status", claim.getStatus().name(), "amount", claim.getAmount().toString()));

        notifyEmployee(claim, approve ? "Reimbursement Approved" : "Reimbursement Rejected",
                approve
                        ? "Your claim " + claim.getClaimNumber() + " for \u20b9" + claim.getAmount() + " has been approved by HR and will be processed for payment."
                        : "Your claim " + claim.getClaimNumber() + " was rejected by HR. " + (remarks != null ? remarks : ""));

        return claim;
    }

    // ─── HR: mark as paid ──────────────────────────────────────────────────────

    @Transactional
    public Reimbursement markPaid(User user, UUID claimId) {
        Reimbursement claim = reimbursementRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Reimbursement claim not found"));

        if (claim.getStatus() != ReimbursementStatus.hr_approved) {
            throw new BadRequestException("Only HR-approved claims can be marked as paid");
        }

        claim.setStatus(ReimbursementStatus.paid);
        claim.setPaidAt(Instant.now());
        claim = reimbursementRepository.save(claim);

        auditService.logAction(user.getId(), "mark_reimbursement_paid", "reimbursement",
                claim.getId().toString(), java.util.Map.of("amount", claim.getAmount().toString()));

        notifyEmployee(claim, "Reimbursement Paid",
                "Your claim " + claim.getClaimNumber() + " for \u20b9" + claim.getAmount() + " has been paid out.");

        return claim;
    }

    // ─── Helper: notify the claim's employee ──────────────────────────────────

    private void notifyEmployee(Reimbursement claim, String title, String message) {
        employeeRepository.findById(claim.getEmployeeId()).ifPresent(emp -> {
            // find their user account via employee id — delegate to NotificationService pattern
            notificationService.notifyEmployeeById(claim.getEmployeeId(), title, message);
        });
    }
}
