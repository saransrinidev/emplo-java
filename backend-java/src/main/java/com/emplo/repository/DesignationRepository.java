package com.emplo.repository;

import com.emplo.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, UUID> {

    List<Designation> findAllByDepartmentId(UUID deptId);

    Optional<Designation> findByTitleAndDepartmentId(String title, UUID deptId);

    List<Designation> findAllByIsActiveTrue();

    List<Designation> findAllByDepartmentIdAndIsActiveTrue(UUID deptId);

    List<Designation> findAllByIsActiveTrueOrderByTitle();
}
