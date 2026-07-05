package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.ForbiddenException;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final EmployeeRepository employeeRepository;

    /**
     * Check if user can view the specified employee's data.
     * HR sees all; employees see only themselves; managers see direct reports.
     */
    public boolean canViewEmployee(User user, UUID employeeId) {
        RoleName role = user.getRole().getName();
        if (role == RoleName.hr_admin) return true;
        if (role == RoleName.employee) return employeeId.equals(user.getEmployeeId());
        if (role == RoleName.manager) {
            if (employeeId.equals(user.getEmployeeId())) return true;
            var target = employeeRepository.findById(employeeId).orElse(null);
            return target != null && user.getEmployeeId() != null
                    && user.getEmployeeId().equals(target.getManagerId());
        }
        return false;
    }

    public void requireViewEmployee(User user, UUID employeeId) {
        if (!canViewEmployee(user, employeeId)) {
            throw new ForbiddenException("Not allowed to view this record");
        }
    }

    public void requireRole(User user, RoleName... roles) {
        RoleName userRole = user.getRole().getName();
        for (RoleName r : roles) {
            if (userRole == r) return;
        }
        throw new ForbiddenException("You do not have permission to perform this action");
    }
}
