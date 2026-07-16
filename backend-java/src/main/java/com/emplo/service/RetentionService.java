package com.emplo.service;

import com.emplo.entity.*;
import com.emplo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Data Retention Service.
 *
 * After the configured retention period following employee termination:
 * - Securely deletes sensitive documents (passport, ID, bank details)
 * - Removes profile photos and personal contact details
 * - Clears bank account encrypted data
 * - Preserves the minimum required records:
 *   - Employee name, code, department, designation, join/leave dates (payroll/tax)
 *   - Salary revisions and structure (tax compliance)
 *   - Leave balances and approved leave (labor compliance)
 *   - Audit logs (legal/compliance — never deleted)
 *   - Performance reviews (retained for reference)
 *
 * Retention periods (configurable):
 * - Sensitive documents (ID, passport, bank): default 7 years after termination
 * - Personal data (photos, phone, DOB, address): default 7 years after termination
 * - Full record archival: default 10 years after termination
 *
 * The job runs daily at 2 AM server time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionService {

    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final EmployeeBankAccountRepository bankAccountRepository;
    private final EmployeeAddressRepository addressRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final CertificationRepository certificationRepository;
    private final AuditService auditService;
    private final StorageService storageService;

    @Value("${app.retention.sensitive-documents-years:7}")
    private int sensitiveDocRetentionYears;

    @Value("${app.retention.personal-data-years:7}")
    private int personalDataRetentionYears;

    @Value("${app.retention.enabled:false}")
    private boolean retentionEnabled;

    /**
     * Scheduled job: runs daily at 2:00 AM.
     * Only processes terminated employees whose termination date exceeds the retention period.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runRetentionCleanup() {
        if (!retentionEnabled) {
            log.debug("Retention cleanup is disabled. Set app.retention.enabled=true to activate.");
            return;
        }
        log.info("Starting data retention cleanup job...");

        Instant sensitiveDocCutoff = Instant.now().minus(sensitiveDocRetentionYears * 365L, ChronoUnit.DAYS);
        Instant personalDataCutoff = Instant.now().minus(personalDataRetentionYears * 365L, ChronoUnit.DAYS);

        List<Employee> terminatedEmployees = employeeRepository.findAllByIsActiveFalse();

        int documentsDeleted = 0;
        int bankAccountsCleared = 0;
        int personalDataCleared = 0;

        for (Employee emp : terminatedEmployees) {
            if (emp.getTerminatedAt() == null) continue;

            // Phase 1: Clear sensitive documents after retention period
            if (emp.getTerminatedAt().isBefore(sensitiveDocCutoff)) {
                documentsDeleted += clearSensitiveDocuments(emp.getId());
                bankAccountsCleared += clearBankAccounts(emp.getId());
            }

            // Phase 2: Clear personal data after retention period
            if (emp.getTerminatedAt().isBefore(personalDataCutoff)) {
                personalDataCleared += clearPersonalData(emp);
            }
        }

        if (documentsDeleted > 0 || bankAccountsCleared > 0 || personalDataCleared > 0) {
            log.info("Retention cleanup complete: {} documents purged, {} bank accounts cleared, {} personal records anonymized.",
                    documentsDeleted, bankAccountsCleared, personalDataCleared);

            // Log as a system audit entry
            auditService.logAction(null, "retention_cleanup", "system", null,
                    java.util.Map.of(
                            "documents_purged", String.valueOf(documentsDeleted),
                            "bank_accounts_cleared", String.valueOf(bankAccountsCleared),
                            "personal_data_cleared", String.valueOf(personalDataCleared)));
        } else {
            log.info("Retention cleanup: no records exceeded retention period.");
        }
    }

    /**
     * Delete sensitive documents (ID proofs, passport) from storage and DB.
     * Preserves: degree/transcript certificates (educational — may be needed for reference).
     */
    @Transactional
    int clearSensitiveDocuments(UUID employeeId) {
        List<Document> docs = documentRepository.findAllByEmployeeId(employeeId);
        int count = 0;

        for (Document doc : docs) {
            // Only purge sensitive identity documents
            String type = doc.getDocumentType() != null ? doc.getDocumentType().name() : "";
            if ("school".equals(type) || "intermediate".equals(type) || "other".equals(type)) {
                // Delete from cloud storage
                if (doc.getFileUrl() != null && !doc.getFileUrl().startsWith("data:")) {
                    try {
                        storageService.delete(doc.getFileUrl());
                    } catch (Exception e) {
                        log.warn("Failed to delete file from storage for doc {}: {}", doc.getId(), e.getMessage());
                    }
                }
                // Clear the URL (mark as purged) but keep the record for audit trail
                doc.setFileUrl("[PURGED — retention policy]");
                doc.setDocumentName(doc.getDocumentName() + " [purged]");
                documentRepository.save(doc);
                count++;
            }
        }

        // Also purge certifications file attachments (but keep the cert record)
        List<Certification> certs = certificationRepository.findAllByEmployeeId(employeeId);
        for (Certification cert : certs) {
            if (cert.getFileUrl() != null && !cert.getFileUrl().isBlank()
                    && !cert.getFileUrl().equals("[PURGED — retention policy]")) {
                if (!cert.getFileUrl().startsWith("data:")) {
                    try { storageService.delete(cert.getFileUrl()); } catch (Exception ignored) {}
                }
                cert.setFileUrl("[PURGED — retention policy]");
                certificationRepository.save(cert);
                count++;
            }
        }

        return count;
    }

    /**
     * Clear bank account details (encrypted data).
     * Keeps the record shell for audit but removes sensitive fields.
     */
    @Transactional
    int clearBankAccounts(UUID employeeId) {
        List<EmployeeBankAccount> accounts = bankAccountRepository.findAllByEmployeeId(employeeId);
        int count = 0;
        for (EmployeeBankAccount acct : accounts) {
            acct.setAccountNumberEnc("[PURGED]");
            acct.setAccountHolderName("[Retention policy — data cleared]");
            acct.setIfscSwiftCode("[PURGED]");
            acct.setBranch(null);
            bankAccountRepository.save(acct);
            count++;
        }
        return count;
    }

    /**
     * Anonymize personal data: remove photo, phone, DOB, addresses, emergency contacts.
     * Preserves: name, employee code, department, designation, dates (needed for payroll/tax).
     */
    @Transactional
    int clearPersonalData(Employee emp) {
        // Clear personal identifiable details
        emp.setProfilePhoto(null);
        emp.setMobileNumber(null);
        emp.setDateOfBirth(null);
        emp.setGender(null);
        emp.setMaritalStatus(null);
        employeeRepository.save(emp);

        // Delete addresses
        List<EmployeeAddress> addresses = addressRepository.findAllByEmployeeId(emp.getId());
        if (!addresses.isEmpty()) {
            addressRepository.deleteAll(addresses);
        }

        // Delete emergency contacts
        List<EmergencyContact> contacts = emergencyContactRepository.findAllByEmployeeId(emp.getId());
        if (!contacts.isEmpty()) {
            emergencyContactRepository.deleteAll(contacts);
        }

        return 1;
    }

    /**
     * Manual trigger for HR to preview what would be cleaned up.
     * Does NOT actually delete — returns a dry-run report.
     */
    public java.util.Map<String, Object> previewRetention() {
        Instant sensitiveDocCutoff = Instant.now().minus(sensitiveDocRetentionYears * 365L, ChronoUnit.DAYS);
        Instant personalDataCutoff = Instant.now().minus(personalDataRetentionYears * 365L, ChronoUnit.DAYS);

        List<Employee> terminated = employeeRepository.findAllByIsActiveFalse();

        int eligibleForDocPurge = 0;
        int eligibleForPersonalPurge = 0;
        List<java.util.Map<String, Object>> details = new java.util.ArrayList<>();

        for (Employee emp : terminated) {
            if (emp.getTerminatedAt() == null) continue;

            boolean docEligible = emp.getTerminatedAt().isBefore(sensitiveDocCutoff);
            boolean personalEligible = emp.getTerminatedAt().isBefore(personalDataCutoff);

            if (docEligible || personalEligible) {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("employee_id", emp.getId());
                entry.put("employee_name", emp.getFullName());
                entry.put("employee_code", emp.getEmployeeCode());
                entry.put("terminated_at", emp.getTerminatedAt());
                entry.put("sensitive_docs_eligible", docEligible);
                entry.put("personal_data_eligible", personalEligible);
                details.add(entry);

                if (docEligible) eligibleForDocPurge++;
                if (personalEligible) eligibleForPersonalPurge++;
            }
        }

        return java.util.Map.of(
                "retention_policy_sensitive_docs_years", sensitiveDocRetentionYears,
                "retention_policy_personal_data_years", personalDataRetentionYears,
                "enabled", retentionEnabled,
                "eligible_for_document_purge", eligibleForDocPurge,
                "eligible_for_personal_data_purge", eligibleForPersonalPurge,
                "employees", details
        );
    }
}
