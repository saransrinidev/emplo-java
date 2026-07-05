package com.emplo.repository;

import com.emplo.entity.OnboardingTask;
import com.emplo.entity.enums.OnboardingTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {

    List<OnboardingTask> findAllByEmployeeIdOrderBySortOrder(UUID employeeId);

    List<OnboardingTask> findAllByEmployeeIdAndStatus(UUID employeeId, OnboardingTaskStatus status);

    long countByEmployeeId(UUID employeeId);

    long countByEmployeeIdAndStatus(UUID employeeId, OnboardingTaskStatus status);

    boolean existsByEmployeeId(UUID employeeId);
}
