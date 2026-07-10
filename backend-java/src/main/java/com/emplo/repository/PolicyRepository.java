package com.emplo.repository;

import com.emplo.entity.Policy;
import com.emplo.entity.enums.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findAllByIsPublishedTrueOrderByCreatedAtDesc();

    List<Policy> findAllByCategoryAndIsPublishedTrueOrderByCreatedAtDesc(PolicyCategory category);

    List<Policy> findAllByOrderByCreatedAtDesc();
}
