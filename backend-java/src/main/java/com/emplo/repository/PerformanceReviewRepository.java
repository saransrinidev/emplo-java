package com.emplo.repository;

import com.emplo.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    List<PerformanceReview> findAllByEmployeeIdOrderByReviewDateDesc(UUID employeeId);

    Optional<PerformanceReview> findFirstByEmployeeIdOrderByReviewDateDesc(UUID employeeId);
}
