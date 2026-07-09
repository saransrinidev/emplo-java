package com.emplo.service;

import com.emplo.entity.*;
import com.emplo.entity.enums.ApprovalStatus;
import com.emplo.entity.enums.DocumentType;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.VerificationStatus;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final CertificationRepository certificationRepository;
    private final DocumentRepository documentRepository;

    private static final List<DocumentType> REQUIRED_DOCUMENT_TYPES = List.of(
            DocumentType.school, DocumentType.intermediate, DocumentType.degree);

    @Cacheable(value = "dashboard_employee", key = "#user.id")
    public Map<String, Object> employeeDashboard(User user) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        String managerName = null;
        if (emp.getManagerId() != null) {
            managerName = employeeRepository.findById(emp.getManagerId())
                    .map(Employee::getFullName).orElse(null);
        }

        SalaryRevision current = salaryRevisionRepository
                .findFirstByEmployeeIdAndApprovalStatusOrderByEffectiveDateDesc(emp.getId(), ApprovalStatus.approved)
                .orElse(null);

        PerformanceReview latestReview = performanceReviewRepository
                .findFirstByEmployeeIdOrderByReviewDateDesc(emp.getId()).orElse(null);

        long certCount = certificationRepository.findAllByEmployeeId(emp.getId()).size();
        LocalDate soon = LocalDate.now().plusDays(90);
        long expiring = certificationRepository.findAllByEmployeeId(emp.getId()).stream()
                .filter(c -> c.getExpiryDate() != null && !c.getExpiryDate().isAfter(soon) && !c.getExpiryDate().isBefore(LocalDate.now()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("designation", emp.getDesignation());
        result.put("date_of_joining", emp.getDateOfJoining() != null ? emp.getDateOfJoining().toString() : null);
        result.put("manager_name", managerName);
        result.put("current_salary", current != null ? current.getRevisedSalary() : null);
        result.put("latest_rating", latestReview != null && latestReview.getRating() != null ? latestReview.getRating().toString() : null);
        result.put("certification_count", certCount);
        result.put("expiring_soon", expiring);
        return result;
    }

    public Map<String, Object> managerDashboard(User user) {
        List<Employee> reports = employeeRepository.findAllByManagerId(user.getEmployeeId());
        List<UUID> reportIds = reports.stream().map(Employee::getId).toList();

        String avgRating = null;
        if (!reportIds.isEmpty()) {
            OptionalDouble avg = performanceReviewRepository.findAll().stream()
                    .filter(r -> reportIds.contains(r.getEmployeeId()) && r.getRating() != null)
                    .mapToDouble(r -> r.getRating().doubleValue())
                    .average();
            if (avg.isPresent()) avgRating = String.format("%.1f", avg.getAsDouble());
        }

        LocalDate soon = LocalDate.now().plusDays(90);
        long certAlerts = !reportIds.isEmpty() ? certificationRepository.findAll().stream()
                .filter(c -> reportIds.contains(c.getEmployeeId()) && c.getExpiryDate() != null
                        && !c.getExpiryDate().isAfter(soon) && !c.getExpiryDate().isBefore(LocalDate.now()))
                .count() : 0;

        Set<UUID> withDocs = documentRepository.findAll().stream()
                .filter(d -> reportIds.contains(d.getEmployeeId()))
                .map(Document::getEmployeeId).collect(Collectors.toSet());
        long missingDocs = reportIds.stream().filter(id -> !withDocs.contains(id)).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("team_members", reports.size());
        result.put("avg_team_rating", avgRating);
        result.put("cert_expiry_alerts", certAlerts);
        result.put("missing_documents", missingDocs);
        result.put("upcoming_anniversaries", 0);
        return result;
    }

    @Cacheable("dashboard_hr")
    public Map<String, Object> hrDashboard() {
        List<Employee> all = employeeRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(e -> "Active".equalsIgnoreCase(e.getEmploymentStatus()) || e.getEmploymentStatus() == null).count();

        LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
        long newJoiners = all.stream()
                .filter(e -> e.getDateOfJoining() != null && !e.getDateOfJoining().isBefore(ninetyDaysAgo))
                .count();

        Map<UUID, Set<DocumentType>> presentMap = new HashMap<>();
        documentRepository.findAll().forEach(d -> presentMap.computeIfAbsent(d.getEmployeeId(), k -> new HashSet<>()).add(d.getDocumentType()));
        long missing = all.stream().filter(e -> REQUIRED_DOCUMENT_TYPES.stream()
                .anyMatch(t -> !presentMap.getOrDefault(e.getId(), Set.of()).contains(t))).count();

        long pending = documentRepository.findAll().stream()
                .filter(d -> d.getStatus() == VerificationStatus.uploaded).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_employees", total);
        result.put("active_employees", active);
        result.put("new_joiners", newJoiners);
        result.put("employees_missing_documents", missing);
        result.put("pending_verifications", pending);
        return result;
    }

    public Map<String, Object> missingDocuments(User user) {
        List<Employee> employees;
        if (user.getRole().getName() == RoleName.manager) {
            employees = employeeRepository.findAllByManagerId(user.getEmployeeId());
        } else {
            employees = employeeRepository.findAll();
        }

        Map<UUID, Set<DocumentType>> presentMap = new HashMap<>();
        documentRepository.findAll().forEach(d -> presentMap.computeIfAbsent(d.getEmployeeId(), k -> new HashSet<>()).add(d.getDocumentType()));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Employee emp : employees) {
            Set<DocumentType> present = presentMap.getOrDefault(emp.getId(), Set.of());
            List<String> missingList = REQUIRED_DOCUMENT_TYPES.stream()
                    .filter(t -> !present.contains(t))
                    .map(t -> t.name())
                    .toList();
            if (!missingList.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", emp.getId().toString());
                item.put("full_name", emp.getFullName());
                item.put("employee_code", emp.getEmployeeCode());
                item.put("department", emp.getDepartment());
                item.put("designation", emp.getDesignation());
                item.put("missing_documents", missingList);
                results.add(item);
            }
        }
        return Map.of("total", results.size(), "employees", results);
    }

    @Cacheable("dashboard_hr")
    public Map<String, Object> analytics() {
        List<Employee> all = employeeRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(e -> {
            String s = e.getEmploymentStatus();
            return s == null || "Active".equalsIgnoreCase(s) || "active".equalsIgnoreCase(s);
        }).count();

        long left = all.stream().filter(e -> {
            String s = e.getEmploymentStatus();
            return "Terminated".equalsIgnoreCase(s) || "Resigned".equalsIgnoreCase(s);
        }).count();
        double attritionRate = total > 0 ? Math.round(left * 1000.0 / total) / 10.0 : 0.0;

        double avgTenure = all.stream()
                .filter(e -> e.getDateOfJoining() != null)
                .mapToLong(e -> ChronoUnit.DAYS.between(e.getDateOfJoining(), LocalDate.now()) / 30)
                .average().orElse(0.0);

        Map<String, Long> deptDist = all.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        List<Map<String, Object>> deptStats = deptDist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> Map.<String, Object>of("department", e.getKey(), "count", e.getValue()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_employees", total);
        result.put("active_employees", active);
        result.put("attrition_rate", attritionRate);
        result.put("avg_tenure_months", Math.round(avgTenure * 10.0) / 10.0);
        result.put("department_distribution", deptStats);
        return result;
    }
}
