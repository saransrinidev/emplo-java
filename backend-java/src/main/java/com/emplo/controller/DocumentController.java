package com.emplo.controller;

import com.emplo.entity.Document;
import com.emplo.entity.User;
import com.emplo.entity.enums.DocumentType;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.VerificationStatus;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.DocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Document>> list(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(documentService.listDocuments(user, employeeId));
    }

    @PostMapping
    public ResponseEntity<Document> upload(@RequestBody UploadDocumentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Document doc = documentService.uploadDocument(user, request.getEmployeeId(),
                request.getDocumentName(), request.getDocumentType(), request.getFileUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<Document> verify(@PathVariable UUID id, @RequestBody VerifyDocumentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(documentService.verifyDocument(user, id, request.getStatus()));
    }

    @Data
    public static class UploadDocumentRequest {
        private UUID employeeId;
        private String documentName;
        private DocumentType documentType;
        private String fileUrl;
    }

    @Data
    public static class VerifyDocumentRequest {
        private VerificationStatus status;
    }
}
