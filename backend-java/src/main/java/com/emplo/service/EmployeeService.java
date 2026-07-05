package com.emplo.service;

import com.emplo.dto.employee.*;
import com.emplo.entity.Employee;
import com.emplo.entity.Role;
import com.emplo.entity.SalaryRevision;
import com.emplo.entity.User;
import com.emplo.entity.enums.ApprovalStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.SalaryRevisionRepository;
import com.emplo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    // ─── List employees ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees(User currentUser, String query) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin, RoleName.manager);

        List<Employee> employees;
        if (query != null && !query.isBlank()) {
            employees = employeeRepository
                    .findAllByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            query, query, query);
        } else {
            employees = employeeRepository.findAll();
        }

        // Managers see only their direct reports
        if (currentUser.getRole().getName() == RoleName.manager) {
            if (currentUser.getEmployeeId() == null) {
                throw new BadRequestException("Manager has no employee record linked");
            }
            UUID managerId = currentUser.getEmployeeId();
            employees = employees.stream()
                    .filter(e -> managerId.equals(e.getManagerId()))
                    .toList();
        }

        return employees.stream().map(this::toEmployeeResponse).toList();
    }

    // ─── List employees with roles (HR only) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeWithRoleResponse> listWithRoles(User currentUser, String query) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        List<Employee> employees;
        if (query != null && !query.isBlank()) {
            employees = employeeRepository
                    .findAllByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            query, query, query);
        } else {
            employees = employeeRepository.findAll();
        }

        return employees.stream().map(emp -> {
            String roleName = userRepository.findByEmployeeId(emp.getId())
                    .map(u -> u.getRole().getName().name())
                    .orElse(null);

            return EmployeeWithRoleResponse.builder()
                    .id(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .fullName(emp.getFullName())
                    .email(emp.getEmail())
                    .department(emp.getDepartment())
                    .designation(emp.getDesignation())
                    .employmentStatus(emp.getEmploymentStatus())
                    .workLocation(emp.getWorkLocation())
                    .managerId(emp.getManagerId())
                    .role(roleName)
                    .profilePhoto(emp.getProfilePhoto())
                    .build();
        }).toList();
    }

    // ─── Get single employee ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeResponse getById(User currentUser, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        // Employees may only see their own record
        if (currentUser.getRole().getName() == RoleName.employee
                && !employeeId.equals(currentUser.getEmployeeId())) {
            throw new ForbiddenException("Not allowed to view this employee");
        }

        return toEmployeeResponse(employee);
    }

    // ─── Create employee ──────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse create(User currentUser, EmployeeCreateRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        if (employeeRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new BadRequestException("Employee code already exists");
        }
        if (request.getManagerId() != null
                && employeeRepository.findById(request.getManagerId()).isEmpty()) {
            throw new BadRequestException("Manager not found");
        }

        Employee employee = Employee.builder()
                .employeeCode(request.getEmployeeCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .maritalStatus(request.getMaritalStatus())
                .dateOfJoining(request.getDateOfJoining())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .managerId(request.getManagerId())
                .employmentStatus(request.getEmploymentStatus())
                .workLocation(request.getWorkLocation())
                .build();

        employee = employeeRepository.save(employee);

        // Create initial salary revision if provided
        if (request.getInitialSalary() != null && request.getInitialSalary().compareTo(BigDecimal.ZERO) > 0) {
            LocalDate effectiveDate = request.getDateOfJoining() != null
                    ? request.getDateOfJoining() : LocalDate.now();

            SalaryRevision revision = SalaryRevision.builder()
                    .employeeId(employee.getId())
                    .effectiveDate(effectiveDate)
                    .previousSalary(null)
                    .revisedSalary(request.getInitialSalary())
                    .revisionPercentage(null)
                    .comments("Initial Salary")
                    .approvalStatus(ApprovalStatus.approved)
                    .createdBy(currentUser.getId())
                    .build();
            salaryRevisionRepository.save(revision);
        }

        // Audit log
        Map<String, Object> changes = new HashMap<>();
        changes.put("full_name", employee.getFullName());
        changes.put("email", employee.getEmail());
        changes.put("initial_salary", request.getInitialSalary() != null ? request.getInitialSalary().toString() : null);
        auditService.logAction(currentUser.getId(), "create", "employee", employee.getId().toString(), changes);

        return toEmployeeResponse(employee);
    }

    // ─── Bulk import employees ────────────────────────────────────────────────────

    @Transactional
    public BulkImportResult bulkImport(User currentUser, BulkImportRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        int created = 0;
        List<String> errors = new ArrayList<>();

        List<BulkImportItem> items = request.getEmployees();
        for (int i = 0; i < items.size(); i++) {
            BulkImportItem item = items.get(i);
            String rowLabel = "Row " + (i + 1) + " (" + item.getEmployeeCode() + ")";

            if (item.getEmployeeCode() == null || item.getEmployeeCode().isBlank()
                    || item.getFullName() == null || item.getFullName().isBlank()
                    || item.getEmail() == null || item.getEmail().isBlank()) {
                errors.add(rowLabel + ": employee_code, full_name, and email are required.");
                continue;
            }

            if (employeeRepository.findByEmployeeCode(item.getEmployeeCode()).isPresent()) {
                errors.add(rowLabel + ": Employee code already exists.");
                continue;
            }

            if (employeeRepository.findByEmail(item.getEmail()).isPresent()) {
                errors.add(rowLabel + ": Email already exists.");
                continue;
            }

            try {
                LocalDate dateOfBirth = parseDate(item.getDateOfBirth(), rowLabel, "date_of_birth", errors);
                LocalDate dateOfJoining = parseDate(item.getDateOfJoining(), rowLabel, "date_of_joining", errors);

                Employee employee = Employee.builder()
                        .employeeCode(item.getEmployeeCode())
                        .fullName(item.getFullName())
                        .email(item.getEmail())
                        .mobileNumber(item.getMobileNumber())
                        .dateOfBirth(dateOfBirth)
                        .gender(item.getGender())
                        .maritalStatus(item.getMaritalStatus())
                        .dateOfJoining(dateOfJoining)
                        .department(item.getDepartment())
                        .designation(item.getDesignation())
                        .employmentStatus(item.getEmploymentStatus())
                        .workLocation(item.getWorkLocation())
                        .build();

                employee = employeeRepository.save(employee);

                // Create initial salary revision if provided
                if (item.getInitialSalary() != null && !item.getInitialSalary().isBlank()) {
                    try {
                        BigDecimal salaryAmount = new BigDecimal(item.getInitialSalary());
                        if (salaryAmount.compareTo(BigDecimal.ZERO) > 0) {
                            LocalDate effectiveDate = dateOfJoining != null ? dateOfJoining : LocalDate.now();
                            SalaryRevision revision = SalaryRevision.builder()
                                    .employeeId(employee.getId())
                                    .effectiveDate(effectiveDate)
                                    .previousSalary(null)
                                    .revisedSalary(salaryAmount)
                                    .revisionPercentage(null)
                                    .comments("Initial Salary")
                                    .approvalStatus(ApprovalStatus.approved)
                                    .createdBy(currentUser.getId())
                                    .build();
                            salaryRevisionRepository.save(revision);
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore invalid salary values
                    }
                }

                created++;
            } catch (Exception e) {
                errors.add(rowLabel + ": " + e.getMessage());
            }
        }

        return BulkImportResult.builder()
                .total(items.size())
                .created(created)
                .errors(errors)
                .build();
    }

    // ─── Update employee ──────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse update(User currentUser, UUID employeeId, EmployeeUpdateRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (request.getManagerId() != null) {
            if (request.getManagerId().equals(employee.getId())) {
                throw new BadRequestException("An employee cannot be their own manager");
            }
            if (employeeRepository.findById(request.getManagerId()).isEmpty()) {
                throw new BadRequestException("Manager not found");
            }
        }

        // Apply only non-null fields
        if (request.getEmployeeCode() != null) employee.setEmployeeCode(request.getEmployeeCode());
        if (request.getFullName() != null) employee.setFullName(request.getFullName());
        if (request.getEmail() != null) employee.setEmail(request.getEmail());
        if (request.getMobileNumber() != null) employee.setMobileNumber(request.getMobileNumber());
        if (request.getDateOfBirth() != null) employee.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) employee.setGender(request.getGender());
        if (request.getMaritalStatus() != null) employee.setMaritalStatus(request.getMaritalStatus());
        if (request.getDateOfJoining() != null) employee.setDateOfJoining(request.getDateOfJoining());
        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
        if (request.getManagerId() != null) employee.setManagerId(request.getManagerId());
        if (request.getEmploymentStatus() != null) employee.setEmploymentStatus(request.getEmploymentStatus());
        if (request.getWorkLocation() != null) employee.setWorkLocation(request.getWorkLocation());
        if (request.getProfilePhoto() != null) employee.setProfilePhoto(request.getProfilePhoto());

        employee = employeeRepository.save(employee);
        return toEmployeeResponse(employee);
    }

    // ─── Create login for employee ────────────────────────────────────────────────

    @Transactional
    public UserAccountResponse createLogin(User currentUser, UUID employeeId, CreateLoginRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        // Check if employee already has a user account
        if (userRepository.findByEmployeeId(employee.getId()).isPresent()) {
            throw new BadRequestException("Employee already has a login account");
        }

        // Check if email is used by another user
        if (userRepository.findByEmail(employee.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered to another account");
        }

        RoleName roleName = request.getRole() != null ? request.getRole() : RoleName.employee;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not found"));

        User user = User.builder()
                .email(employee.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roleId(role.getId())
                .employeeId(employee.getId())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        return UserAccountResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(role.getName().name())
                .employeeId(employee.getId().toString())
                .build();
    }

    // ─── Bulk create logins ───────────────────────────────────────────────────────

    @Transactional
    public BulkCreateLoginResult bulkCreateLogin(User currentUser, BulkCreateLoginRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        RoleName roleName = request.getRole() != null ? request.getRole() : RoleName.employee;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not found"));

        int created = 0;
        List<String> errors = new ArrayList<>();

        for (UUID empId : request.getEmployeeIds()) {
            Optional<Employee> empOpt = employeeRepository.findById(empId);
            if (empOpt.isEmpty()) {
                errors.add(empId + ": Employee not found");
                continue;
            }
            Employee employee = empOpt.get();

            if (userRepository.findByEmployeeId(empId).isPresent()) {
                errors.add(employee.getFullName() + ": Already has a login account");
                continue;
            }

            if (userRepository.findByEmail(employee.getEmail()).isPresent()) {
                errors.add(employee.getFullName() + ": Email already in use by another account");
                continue;
            }

            User user = User.builder()
                    .email(employee.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .roleId(role.getId())
                    .employeeId(employee.getId())
                    .isActive(true)
                    .build();
            userRepository.save(user);
            created++;
        }

        return BulkCreateLoginResult.builder()
                .total(request.getEmployeeIds().size())
                .created(created)
                .errors(errors)
                .build();
    }

    // ─── Delete (terminate) employee ──────────────────────────────────────────────

    @Transactional
    public void delete(User currentUser, UUID employeeId) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        // Audit log the termination
        Map<String, Object> changes = new HashMap<>();
        changes.put("full_name", employee.getFullName());
        changes.put("email", employee.getEmail());
        auditService.logAction(currentUser.getId(), "terminate", "employee", employee.getId().toString(), changes);

        // 1. Nullify other employees reporting to this one
        entityManager.createNativeQuery("UPDATE employees SET manager_id = NULL WHERE manager_id = :eid")
                .setParameter("eid", employee.getId())
                .executeUpdate();

        // 2. Nullify leave_requests.manager_id and performance_reviews.reviewer_id
        entityManager.createNativeQuery("UPDATE leave_requests SET manager_id = NULL WHERE manager_id = :eid")
                .setParameter("eid", employee.getId())
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE performance_reviews SET reviewer_id = NULL WHERE reviewer_id = :eid")
                .setParameter("eid", employee.getId())
                .executeUpdate();

        // 3. Delete linked user account and clean up all user FK references
        Optional<User> linkedUserOpt = userRepository.findByEmployeeId(employee.getId());
        if (linkedUserOpt.isPresent()) {
            User linkedUser = linkedUserOpt.get();
            UUID uid = linkedUser.getId();

            // Nullify all FK references to this user across the system
            entityManager.createNativeQuery("UPDATE salary_revisions SET created_by = NULL WHERE created_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE audit_logs SET actor_id = NULL WHERE actor_id = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE edit_access_requests SET approved_by = NULL WHERE approved_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE edit_access_requests SET confirmed_by = NULL WHERE confirmed_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE employee_edit_permissions SET granted_by = NULL WHERE granted_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE documents SET verified_by = NULL WHERE verified_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE tickets SET assigned_to = NULL WHERE assigned_to = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE tickets SET resolved_by = NULL WHERE resolved_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE profile_change_requests SET reviewed_by = NULL WHERE reviewed_by = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE leave_requests SET hr_id = NULL WHERE hr_id = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();

            // Delete owned records that won't cascade
            entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM ticket_comments WHERE user_id = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();

            userRepository.delete(linkedUser);
            entityManager.flush();
        }

        // 4. Delete the employee (cascades to addresses, contacts, docs, certs, leave_requests, etc.)
        employeeRepository.delete(employee);
    }

    // ─── Change employee role ─────────────────────────────────────────────────────

    @Transactional
    public UserAccountResponse changeRole(User currentUser, UUID employeeId, ChangeRoleRequest request) {
        authorizationService.requireRole(currentUser, RoleName.hr_admin);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        User user = userRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new BadRequestException("Employee has no login account"));

        String oldRole = user.getRole().getName().name();

        Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BadRequestException("Role not found"));

        user.setRoleId(newRole.getId());
        user = userRepository.save(user);

        // Refresh role reference
        entityManager.refresh(user);

        // Audit log
        Map<String, Object> changes = new HashMap<>();
        changes.put("from", oldRole);
        changes.put("to", request.getRole().name());
        auditService.logAction(currentUser.getId(), "change_role", "employee", employee.getId().toString(), changes);

        return UserAccountResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(newRole.getName().name())
                .employeeId(employee.getId().toString())
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .mobileNumber(employee.getMobileNumber())
                .dateOfBirth(employee.getDateOfBirth())
                .gender(employee.getGender())
                .maritalStatus(employee.getMaritalStatus())
                .profilePhoto(employee.getProfilePhoto())
                .dateOfJoining(employee.getDateOfJoining())
                .department(employee.getDepartment())
                .designation(employee.getDesignation())
                .managerId(employee.getManagerId())
                .employmentStatus(employee.getEmploymentStatus())
                .workLocation(employee.getWorkLocation())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    private LocalDate parseDate(String value, String rowLabel, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            errors.add(rowLabel + ": Invalid date format for " + fieldName + ": '" + value + "' (use YYYY-MM-DD)");
            return null;
        }
    }
}
