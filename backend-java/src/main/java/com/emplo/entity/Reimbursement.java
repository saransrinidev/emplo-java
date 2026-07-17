package com.emplo.entity;

import com.emplo.entity.enums.ReimbursementCategory;
import com.emplo.entity.enums.ReimbursementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Employee expense reimbursement claim.
 *
 * Flow:
 * 1. Employee submits claim (name, amount, bill/receipt, category, description) -> status: pending
 * 2. Manager reviews and gives "assurance" (approve/reject) as an extra check -> manager_approved / manager_rejected
 * 3. Only if manager approved: HR makes the final decision -> hr_approved / hr_rejected
 * 4. HR marks as paid once processed -> paid
 */
@Entity
@Table(name = "reimbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reimbursement {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 20)
    private String claimNumber;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    /** Name entered on the claim (defaults to employee's full name, but editable) */
    @Column(name = "claimant_name", nullable = false, length = 255)
    private String claimantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReimbursementCategory category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    /** URL of the uploaded bill/receipt (cloud storage) */
    @Column(name = "bill_url", columnDefinition = "TEXT")
    private String billUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReimbursementStatus status = ReimbursementStatus.pending;

    // Manager assurance step
    @Column(name = "manager_id", columnDefinition = "uuid")
    private UUID managerId;

    @Column(name = "manager_remarks", length = 500)
    private String managerRemarks;

    @Column(name = "manager_acted_at")
    private Instant managerActedAt;

    // HR final decision step
    @Column(name = "hr_id", columnDefinition = "uuid")
    private UUID hrId;

    @Column(name = "hr_remarks", length = 500)
    private String hrRemarks;

    @Column(name = "hr_acted_at")
    private Instant hrActedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
