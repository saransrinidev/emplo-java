package com.emplo.repository;

import com.emplo.entity.OnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, UUID> {

    List<OnboardingTemplate> findAllByIsActiveTrueOrderBySortOrder();
}
