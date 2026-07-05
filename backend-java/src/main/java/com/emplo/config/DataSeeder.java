package com.emplo.config;

import com.emplo.entity.Employee;
import com.emplo.entity.OnboardingTemplate;
import com.emplo.entity.Role;
import com.emplo.entity.SalaryRevision;
import com.emplo.entity.User;
import com.emplo.entity.enums.ApprovalStatus;
import com.emplo.entity.enums.OnboardingCategory;
import com.emplo.entity.enums.RoleName;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.OnboardingTemplateRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.SalaryRevisionRepository;
import com.emplo.repository.UserRepository;
import com.emplo.service.SalaryStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OnboardingTemplateRepository onboardingTemplateRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final SalaryStructureService salaryStructureService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedHrAdmin();
        seedOnboardingTemplates();
        backfillSalaryStructures();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(roleName.name())
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void seedHrAdmin() {
        String email = "saransrini@company.com";
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("HR admin already exists. Skipping seed.");
            return;
        }

        Role hrRole = roleRepository.findByName(RoleName.hr_admin)
                .orElseThrow(() -> new RuntimeException("HR admin role not found"));

        Employee emp = Employee.builder()
                .employeeCode("EMP-0001")
                .fullName("Saransrini")
                .email(email)
                .dateOfJoining(LocalDate.now())
                .department("Human Resources")
                .designation("HR Administrator")
                .employmentStatus("Active")
                .build();
        emp = employeeRepository.save(emp);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Secret123"))
                .roleId(hrRole.getId())
                .employeeId(emp.getId())
                .isActive(true)
                .build();
        userRepository.save(user);

        log.info("Seed complete. Login: saransrini@company.com / Secret123");
    }

    private void seedOnboardingTemplates() {
        if (onboardingTemplateRepository.count() > 0) {
            log.info("Onboarding templates already exist. Skipping.");
            return;
        }

        int order = 0;

        // Personal Information
        save("Complete your personal profile", "Fill in your date of birth, gender, marital status, and contact details.",
                OnboardingCategory.personal_info, ++order, true, 3, "fill_form", "/profile");
        save("Upload profile photo", "Add a professional photo to your profile.",
                OnboardingCategory.personal_info, ++order, false, 3, "fill_form", "/profile");
        save("Add emergency contacts", "Provide at least one emergency contact for company records.",
                OnboardingCategory.personal_info, ++order, true, 5, "fill_form", "/profile");
        save("Update your address", "Add your current and permanent address details.",
                OnboardingCategory.personal_info, ++order, true, 5, "fill_form", "/profile");

        // Documents
        save("Upload Aadhaar / National ID", "Upload a scanned copy of your government-issued ID.",
                OnboardingCategory.documents, ++order, true, 7, "upload_document", "/documents");
        save("Upload PAN Card", "Required for tax and payroll processing.",
                OnboardingCategory.documents, ++order, true, 7, "upload_document", "/documents");
        save("Upload 10th Certificate", "School leaving certificate for educational verification.",
                OnboardingCategory.documents, ++order, true, 14, "upload_document", "/documents");
        save("Upload 12th Certificate", "Intermediate/higher secondary certificate.",
                OnboardingCategory.documents, ++order, true, 14, "upload_document", "/documents");
        save("Upload Degree Certificate", "Graduation or highest qualification certificate.",
                OnboardingCategory.documents, ++order, true, 14, "upload_document", "/documents");
        save("Upload Previous Employment Letter", "Relieving/experience letter from your last employer.",
                OnboardingCategory.documents, ++order, false, 14, "upload_document", "/documents");

        // IT Setup
        save("Set up your email account", "Configure your company email on your devices.",
                OnboardingCategory.it_setup, ++order, true, 2, "acknowledge", null);
        save("Install required tools", "Install Slack, Teams, IDE, or other tools as per your team.",
                OnboardingCategory.it_setup, ++order, true, 3, "acknowledge", null);
        save("Set up VPN access", "Configure VPN for secure remote access if applicable.",
                OnboardingCategory.it_setup, ++order, false, 5, "acknowledge", null);

        // Compliance
        save("Read and acknowledge Employee Handbook", "Review company policies, code of conduct, and leave policies.",
                OnboardingCategory.compliance, ++order, true, 7, "acknowledge", null);
        save("Sign NDA / Confidentiality Agreement", "Complete the non-disclosure agreement.",
                OnboardingCategory.compliance, ++order, true, 3, "acknowledge", null);
        save("Complete Anti-Harassment Training", "Mandatory workplace training module.",
                OnboardingCategory.compliance, ++order, true, 14, "link", null);

        // Team Introduction
        save("Meet your manager", "Schedule an intro meeting with your reporting manager.",
                OnboardingCategory.team_intro, ++order, true, 3, "acknowledge", null);
        save("Meet your team members", "Get introduced to your immediate team.",
                OnboardingCategory.team_intro, ++order, true, 5, "acknowledge", null);
        save("Attend department orientation", "Join the department-level orientation session.",
                OnboardingCategory.team_intro, ++order, false, 7, "acknowledge", null);

        // Training
        save("Complete company orientation", "Attend the general company orientation session.",
                OnboardingCategory.training, ++order, true, 7, "acknowledge", null);
        save("Complete role-specific training", "Finish any mandatory training for your position.",
                OnboardingCategory.training, ++order, true, 30, "acknowledge", null);

        log.info("Seeded {} onboarding templates.", order);
    }

    private void save(String title, String description, OnboardingCategory category,
                      int sortOrder, boolean required, int dueDays, String actionType, String actionUrl) {
        onboardingTemplateRepository.save(OnboardingTemplate.builder()
                .title(title)
                .description(description)
                .category(category)
                .sortOrder(sortOrder)
                .isRequired(required)
                .isActive(true)
                .dueDays(dueDays)
                .actionType(actionType)
                .actionUrl(actionUrl)
                .build());
    }

    private void backfillSalaryStructures() {
        List<Employee> allEmployees = employeeRepository.findAll();
        int generated = 0;

        for (Employee emp : allEmployees) {
            // Skip if already has a structure
            if (salaryStructureService.getSalaryStructureByEmployeeId(emp.getId()) != null) {
                continue;
            }

            // Find their latest approved salary revision
            SalaryRevision revision = salaryRevisionRepository
                    .findFirstByEmployeeIdAndApprovalStatusOrderByEffectiveDateDesc(
                            emp.getId(), ApprovalStatus.approved)
                    .orElse(null);

            if (revision != null && revision.getRevisedSalary() != null
                    && revision.getRevisedSalary().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Treat revised_salary as annual CTC
                    salaryStructureService.autoGenerateStructure(emp.getId(), revision.getRevisedSalary());
                    generated++;
                } catch (Exception ignored) {
                }
            }
        }

        if (generated > 0) {
            log.info("Backfilled salary structures for {} existing employees.", generated);
        }
    }
}
