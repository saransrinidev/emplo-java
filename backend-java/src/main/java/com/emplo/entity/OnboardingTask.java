package com.emplo.entity;

import com.emplo.entity.enums.OnboardingCategory;
import com.emplo.entity.enums.OnboardingTaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-employee onboarding checklist item.
 * Created from templates when an employee begins onboarding.
 */
@Entity
@Table(name = "onboarding_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTask {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    @Column(name = "template_id", columnDefinition = "uuid")
    private UUID templateId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnboardingCategory category;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OnboardingTaskStatus status = OnboardingTaskStatus.pending;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", columnDefinition = "uuid")
    private UUID completedBy; // user who marked it complete (employee or HR)

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(length = 500)
    private String notes; // Employee can add a note when completing

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
