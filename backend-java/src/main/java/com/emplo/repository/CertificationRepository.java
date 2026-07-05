package com.emplo.repository;

import com.emplo.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    List<Certification> findAllByEmployeeId(UUID employeeId);

    List<Certification> findAllByExpiryDateBetween(LocalDate start, LocalDate end);

    List<Certification> findAllByExpiryDateNotNullAndExpiryDateBefore(LocalDate date);

    long countByEmployeeId(UUID employeeId);
}
