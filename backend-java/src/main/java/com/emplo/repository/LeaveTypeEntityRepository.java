package com.emplo.repository;

import com.emplo.entity.LeaveTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeEntityRepository extends JpaRepository<LeaveTypeEntity, UUID> {

    Optional<LeaveTypeEntity> findByCode(String code);

    Optional<LeaveTypeEntity> findByName(String name);
}
