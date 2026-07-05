package com.emplo.service;

import com.emplo.entity.Certification;
import com.emplo.entity.Employee;
import com.emplo.entity.User;
import com.emplo.entity.enums.CertificationCategory;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.VerificationStatus;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.CertificationRepository;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    public List<Certification> listCertifications(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        authorizationService.requireViewEmployee(user, target);
        return certificationRepository.findAllByEmployeeId(target);
    }

    public List<Certification> getExpiring(int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return certificationRepository.findAllByExpiryDateNotNullAndExpiryDateBefore(cutoff);
    }

    @Transactional
    public Certification addCertification(User user, UUID employeeId, String certificateName,
                                           String certificateNumber, CertificationCategory category,
                                           LocalDate issuedDate, LocalDate expiryDate, String fileUrl) {
        UUID targetEmployeeId = employeeId != null ? employeeId : user.getEmployeeId();
        if (targetEmployeeId == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        if (user.getRole().getName() == RoleName.employee && !targetEmployeeId.equals(user.getEmployeeId())) {
            throw new ForbiddenException("Cannot add for another employee");
        }

        Certification cert = Certification.builder()
                .employeeId(targetEmployeeId)
                .certificateName(certificateName)
                .certificateNumber(certificateNumber)
                .category(category != null ? category : CertificationCategory.other)
                .issuedDate(issuedDate)
                .expiryDate(expiryDate)
                .fileUrl(fileUrl)
                .verificationStatus(VerificationStatus.uploaded)
                .build();
        cert = certificationRepository.save(cert);

        auditService.logAction(user.getId(), "add_certification", "certification",
                cert.getId().toString(), Map.of("certificate_name", certificateName));

        if (user.getRole().getName() != RoleName.hr_admin) {
            Employee emp = employeeRepository.findById(targetEmployeeId).orElse(null);
            String empName = emp != null ? emp.getFullName() : "An employee";
            notificationService.notifyHrAndManager(user, "Certification Added",
                    empName + " added a certification: " + certificateName);
        }

        return cert;
    }

    @Transactional
    public Certification updateCertification(User user, UUID certId, Map<String, Object> updates) {
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new NotFoundException("Certification not found"));

        if (updates.containsKey("verificationStatus")) {
            cert.setVerificationStatus(VerificationStatus.valueOf((String) updates.get("verificationStatus")));
        }
        if (updates.containsKey("certificateName")) {
            cert.setCertificateName((String) updates.get("certificateName"));
        }
        if (updates.containsKey("certificateNumber")) {
            cert.setCertificateNumber((String) updates.get("certificateNumber"));
        }
        if (updates.containsKey("expiryDate") && updates.get("expiryDate") != null) {
            cert.setExpiryDate(LocalDate.parse((String) updates.get("expiryDate")));
        }
        if (updates.containsKey("issuedDate") && updates.get("issuedDate") != null) {
            cert.setIssuedDate(LocalDate.parse((String) updates.get("issuedDate")));
        }
        if (updates.containsKey("category")) {
            cert.setCategory(CertificationCategory.valueOf((String) updates.get("category")));
        }
        if (updates.containsKey("fileUrl")) {
            cert.setFileUrl((String) updates.get("fileUrl"));
        }

        cert = certificationRepository.save(cert);
        auditService.logAction(user.getId(), "verify_certification", "certification",
                cert.getId().toString(), updates);
        return cert;
    }
}
