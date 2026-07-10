package com.emplo.repository;

import com.emplo.entity.PolicyAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyAcknowledgementRepository extends JpaRepository<PolicyAcknowledgement, UUID> {

    List<PolicyAcknowledgement> findAllByEmployeeId(UUID employeeId);

    List<PolicyAcknowledgement> findAllByPolicyId(UUID policyId);

    Optional<PolicyAcknowledgement> findByPolicyIdAndEmployeeId(UUID policyId, UUID employeeId);

    long countByPolicyId(UUID policyId);
}
