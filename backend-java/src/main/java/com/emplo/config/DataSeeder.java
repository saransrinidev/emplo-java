package com.emplo.config;

import com.emplo.entity.Employee;
import com.emplo.entity.Role;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedHrAdmin();
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

        // Create employee record
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

        // Create user login
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Secret123"))
                .roleId(hrRole.getId())
                .employeeId(emp.getId())
                .isActive(true)
                .build();
        userRepository.save(user);

        log.info("Seed complete. Login (password: Secret123): saransrini@company.com (hr_admin)");
    }
}
