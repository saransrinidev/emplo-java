package com.emplo.repository;

import com.emplo.entity.Document;
import com.emplo.entity.enums.DocumentType;
import com.emplo.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByEmployeeId(UUID employeeId);

    Optional<Document> findByEmployeeIdAndDocumentType(UUID employeeId, DocumentType type);

    long countByStatus(VerificationStatus status);
}
