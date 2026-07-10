package com.emplo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks that a specific employee has read & acknowledged a specific policy version.
 */
@Entity
@Table(name = "policy_acknowledgements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"policy_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAcknowledgement {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "policy_id", nullable = false, columnDefinition = "uuid")
    private UUID policyId;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    @CreationTimestamp
    @Column(name = "acknowledged_at", nullable = false, updatable = false)
    private Instant acknowledgedAt;
}
