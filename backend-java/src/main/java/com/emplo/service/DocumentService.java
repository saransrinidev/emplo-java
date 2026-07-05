package com.emplo.service;

import com.emplo.entity.Document;
import com.emplo.entity.Employee;
import com.emplo.entity.User;
import com.emplo.entity.enums.DocumentType;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.VerificationStatus;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.DocumentRepository;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<Document> listDocuments(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        return documentRepository.findAllByEmployeeId(target);
    }

    @Transactional
    public Document uploadDocument(User user, UUID employeeId, String documentName,
                                   DocumentType documentType, String fileUrl) {
        UUID targetEmployeeId = employeeId != null ? employeeId : user.getEmployeeId();
        if (targetEmployeeId == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        if (user.getRole().getName() == RoleName.employee && !targetEmployeeId.equals(user.getEmployeeId())) {
            throw new ForbiddenException("Cannot upload for another employee");
        }

        // Check for existing document of same type (re-upload)
        Document existing = documentRepository.findByEmployeeIdAndDocumentType(targetEmployeeId, documentType)
                .orElse(null);

        Document doc;
        if (existing != null && documentType != DocumentType.other) {
            existing.setDocumentName(documentName != null ? documentName : existing.getDocumentName());
            existing.setFileUrl(fileUrl);
            existing.setStatus(VerificationStatus.uploaded);
            existing.setVerifiedBy(null);
            existing.setVerifiedAt(null);
            doc = documentRepository.save(existing);
            auditService.logAction(user.getId(), "reupload", "document",
                    doc.getId().toString(), Map.of("document_name", doc.getDocumentName() != null ? doc.getDocumentName() : "", "document_type", documentType.name()));
        } else {
            doc = Document.builder()
                    .employeeId(targetEmployeeId)
                    .documentName(documentName)
                    .documentType(documentType)
                    .fileUrl(fileUrl)
                    .status(VerificationStatus.uploaded)
                    .build();
            doc = documentRepository.save(doc);
            auditService.logAction(user.getId(), "upload", "document",
                    doc.getId().toString(), Map.of("document_name", documentName != null ? documentName : "", "document_type", documentType.name()));
        }

        if (user.getRole().getName() != RoleName.hr_admin) {
            Employee emp = employeeRepository.findById(targetEmployeeId).orElse(null);
            String empName = emp != null ? emp.getFullName() : "An employee";
            notificationService.notifyHrAndManager(user, "Document Uploaded",
                    empName + " uploaded a document: " + (documentName != null ? documentName : documentType.name()));
        }

        return doc;
    }

    @Transactional
    public Document verifyDocument(User user, UUID docId, VerificationStatus status) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        doc.setStatus(status);
        doc.setVerifiedBy(user.getId());
        doc.setVerifiedAt(Instant.now());
        doc = documentRepository.save(doc);

        auditService.logAction(user.getId(), "verify_" + status.name(), "document",
                doc.getId().toString(), Map.of("status", status.name()));

        return doc;
    }
}
