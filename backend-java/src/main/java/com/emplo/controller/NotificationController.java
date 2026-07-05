package com.emplo.controller;

import com.emplo.entity.Employee;
import com.emplo.entity.Notification;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.NotificationRepository;
import com.emplo.repository.UserRepository;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> list() {
        User user = currentUserProvider.getCurrentUser();
        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(notifications.stream().limit(50).toList());
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markAllRead() {
        User user = currentUserProvider.getCurrentUser();
        List<Notification> unread = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().filter(n -> !n.getIsRead()).toList();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        User user = currentUserProvider.getCurrentUser();
        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/send-alert")
    public ResponseEntity<Map<String, Object>> sendAlert(@RequestBody SendAlertRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);

        UUID empId = UUID.fromString(request.getEmployeeId());
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        List<String> sentTo = new ArrayList<>();

        userRepository.findByEmployeeId(empId).ifPresent(empUser -> {
            notificationService.createNotification(empUser.getId(), request.getTitle(), request.getMessage());
            sentTo.add(employee.getEmail());
        });

        if (request.isNotifyManager() && employee.getManagerId() != null) {
            userRepository.findByEmployeeId(employee.getManagerId()).ifPresent(mgrUser -> {
                notificationService.createNotification(mgrUser.getId(), request.getTitle(),
                        "[Re: " + employee.getFullName() + "] " + request.getMessage());
                employeeRepository.findById(employee.getManagerId()).ifPresent(mgr -> sentTo.add(mgr.getEmail()));
            });
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sent_to", sentTo));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markOneRead(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(user.getId())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteOne(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(user.getId())) {
                notificationRepository.delete(n);
            }
        });
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void clearAll() {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.deleteAllByUserId(user.getId());
    }

    @Data
    public static class SendAlertRequest {
        private String employeeId;
        private String title;
        private String message;
        private boolean notifyManager = false;
    }
}
