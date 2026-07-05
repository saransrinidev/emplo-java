package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.SalaryRevision;
import com.emplo.entity.User;
import com.emplo.entity.enums.ApprovalStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.SalaryRevisionRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryRevisionRepository salaryRevisionRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    public List<SalaryRevision> getHistory(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        authorizationService.requireViewEmployee(user, target);
        return salaryRevisionRepository.findAllByEmployeeIdOrderByEffectiveDateDesc(target);
    }

    public SalaryRevision getCurrentSalary(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return null;
        authorizationService.requireViewEmployee(user, target);
        return getLatestApproved(target);
    }

    public List<SalaryRevision> getPendingRevisions() {
        return salaryRevisionRepository.findAllByApprovalStatusOrderByEffectiveDateDesc(ApprovalStatus.pending);
    }

    @Transactional
    public SalaryRevision createRevision(User user, UUID employeeId, BigDecimal revisedSalary,
                                          String effectiveDate, BigDecimal previousSalary,
                                          BigDecimal revisionPercentage, String comments) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        if (revisedSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Revised salary must be greater than zero");
        }

        SalaryRevision latest = getLatestApproved(employeeId);

        if (previousSalary == null && latest != null) {
            previousSalary = latest.getRevisedSalary();
        }
        if (revisionPercentage == null && previousSalary != null && previousSalary.compareTo(BigDecimal.ZERO) > 0) {
            revisionPercentage = revisedSalary.subtract(previousSalary)
                    .divide(previousSalary, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        ApprovalStatus status = (latest == null) ? ApprovalStatus.approved : ApprovalStatus.pending;
        String finalComments = comments;
        if (latest == null && (finalComments == null || finalComments.isEmpty())) {
            finalComments = "Initial Salary";
        }

        SalaryRevision revision = SalaryRevision.builder()
                .employeeId(employeeId)
                .effectiveDate(java.time.LocalDate.parse(effectiveDate))
                .previousSalary(previousSalary)
                .revisedSalary(revisedSalary)
                .revisionPercentage(revisionPercentage)
                .comments(finalComments)
                .approvalStatus(status)
                .createdBy(user.getId())
                .build();
        revision = salaryRevisionRepository.save(revision);

        auditService.logAction(user.getId(), "propose_salary_revision", "salary",
                revision.getId().toString(), Map.of(
                        "employee_id", employeeId.toString(),
                        "employee_name", emp.getFullName(),
                        "revised_salary", revisedSalary.toString()));

        notificationService.notifyHrOnly(user, "Salary Revision Proposed",
                "A salary revision for " + emp.getFullName() + " has been proposed.");

        return revision;
    }

    @Transactional
    public SalaryRevision approveRevision(User user, UUID revisionId) {
        SalaryRevision revision = salaryRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Salary revision not found"));
        if (revision.getApprovalStatus() != ApprovalStatus.pending) {
            throw new BadRequestException("Revision is already " + revision.getApprovalStatus().name());
        }
        revision.setApprovalStatus(ApprovalStatus.approved);
        revision = salaryRevisionRepository.save(revision);

        auditService.logAction(user.getId(), "approve_salary_revision", "salary",
                revision.getId().toString(), Map.of("status", "approved"));

        final SalaryRevision savedRevision = revision;
        userRepository.findByEmployeeId(savedRevision.getEmployeeId()).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(), "Salary Revision Approved",
                        "Your salary has been revised to ₹" + savedRevision.getRevisedSalary() + " effective " + savedRevision.getEffectiveDate()));

        return revision;
    }

    @Transactional
    public SalaryRevision rejectRevision(User user, UUID revisionId) {
        SalaryRevision revision = salaryRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Salary revision not found"));
        if (revision.getApprovalStatus() != ApprovalStatus.pending) {
            throw new BadRequestException("Revision is already " + revision.getApprovalStatus().name());
        }
        revision.setApprovalStatus(ApprovalStatus.rejected);
        revision = salaryRevisionRepository.save(revision);

        auditService.logAction(user.getId(), "reject_salary_revision", "salary",
                revision.getId().toString(), Map.of("status", "rejected"));

        return revision;
    }

    private SalaryRevision getLatestApproved(UUID employeeId) {
        return salaryRevisionRepository.findFirstByEmployeeIdAndApprovalStatusOrderByEffectiveDateDesc(
                employeeId, ApprovalStatus.approved).orElse(null);
    }
}
