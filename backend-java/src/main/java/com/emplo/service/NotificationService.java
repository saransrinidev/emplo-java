package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.Notification;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.NotificationRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    public void createNotification(UUID userId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * Notify all HR admins and the actor's manager.
     * Does NOT notify the actor themselves.
     */
    public void notifyHrAndManager(User actorUser, String title, String message) {
        // Notify all HR admins
        var hrRole = roleRepository.findByName(RoleName.hr_admin);
        if (hrRole.isPresent()) {
            List<User> hrUsers = userRepository.findAllByRoleId(hrRole.get().getId());
            for (User hrUser : hrUsers) {
                if (!hrUser.getId().equals(actorUser.getId())) {
                    createNotification(hrUser.getId(), title, message);
                }
            }
        }

        // Notify actor's manager
        if (actorUser.getEmployeeId() != null) {
            employeeRepository.findById(actorUser.getEmployeeId()).ifPresent(employee -> {
                if (employee.getManagerId() != null) {
                    userRepository.findByEmployeeId(employee.getManagerId()).ifPresent(mgrUser -> {
                        if (!mgrUser.getId().equals(actorUser.getId())) {
                            createNotification(mgrUser.getId(), title, message);
                        }
                    });
                }
            });
        }
    }

    /**
     * Notify only HR admins (when a manager does something).
     */
    public void notifyHrOnly(User actorUser, String title, String message) {
        var hrRole = roleRepository.findByName(RoleName.hr_admin);
        if (hrRole.isPresent()) {
            List<User> hrUsers = userRepository.findAllByRoleId(hrRole.get().getId());
            for (User hrUser : hrUsers) {
                if (!hrUser.getId().equals(actorUser.getId())) {
                    createNotification(hrUser.getId(), title, message);
                }
            }
        }
    }

    /**
     * Directly notify the user account linked to a given employee ID.
     * Used for system-initiated notifications (e.g. workflow status changes)
     * where there's no "actor" to exclude.
     */
    public void notifyEmployeeById(UUID employeeId, String title, String message) {
        userRepository.findByEmployeeId(employeeId).ifPresent(empUser ->
                createNotification(empUser.getId(), title, message));
    }
}
