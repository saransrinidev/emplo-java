package com.emplo.service;

import com.emplo.entity.EmployeeEditPermission;
import com.emplo.entity.User;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeEditPermissionRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final EmployeeEditPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public EmployeeEditPermission grantPermission(User user, UUID employeeId, EditableSection section,
                                                    Instant startAt, Instant expiryAt) {
        if (expiryAt.isBefore(startAt) || expiryAt.equals(startAt)) {
            throw new BadRequestException("expiry_at must be after start_at");
        }
        EmployeeEditPermission perm = EmployeeEditPermission.builder()
                .employeeId(employeeId)
                .section(section)
                .grantedBy(user.getId())
                .startAt(startAt)
                .expiryAt(expiryAt)
                .isRevoked(false)
                .build();
        perm = permissionRepository.save(perm);

        auditService.logAction(user.getId(), "grant_permission", "permission",
                perm.getId().toString(), Map.of("employee_id", employeeId.toString(), "section", section.name()));

        userRepository.findByEmployeeId(employeeId).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(), "Temporary Edit Access Granted",
                        "You can now edit your " + section.name() + " until " + expiryAt));

        return perm;
    }

    public List<EmployeeEditPermission> listPermissions(User user, UUID employeeId) {
        if (user.getRole().getName() == RoleName.employee) {
            return permissionRepository.findAllByEmployeeIdOrderByExpiryAtDesc(user.getEmployeeId());
        }
        if (employeeId != null) {
            return permissionRepository.findAllByEmployeeIdOrderByExpiryAtDesc(employeeId);
        }
        return permissionRepository.findAll();
    }

    @Transactional
    public void revokePermission(User user, UUID permId) {
        EmployeeEditPermission perm = permissionRepository.findById(permId)
                .orElseThrow(() -> new NotFoundException("Permission not found"));
        perm.setIsRevoked(true);
        permissionRepository.save(perm);

        auditService.logAction(user.getId(), "revoke_permission", "permission",
                perm.getId().toString(), Map.of("employee_id", perm.getEmployeeId().toString(), "section", perm.getSection().name()));
    }
}
