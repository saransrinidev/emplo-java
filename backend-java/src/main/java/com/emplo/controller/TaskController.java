package com.emplo.controller;

import com.emplo.entity.Task;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.TaskPriority;
import com.emplo.entity.enums.TaskStatus;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.TaskService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody CreateTaskRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        Task task = taskService.createTask(user, request.getTitle(), request.getDescription(),
                request.getAssignedTo(), request.getDueDate(), request.getPriority());
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Task>> myTasks(
            @RequestParam(value = "status", required = false) TaskStatus status) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.myTasks(user, status));
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<Task>> assignedTasks(
            @RequestParam(value = "status", required = false) TaskStatus status) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(taskService.assignedTasks(user, status));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Task> complete(@PathVariable UUID id, @RequestBody CompleteTaskRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.completeTask(user, id, request.getCompletionNote()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(taskService.updateStatus(user, id, request.getStatus()));
    }

    @Data
    public static class CreateTaskRequest {
        private String title;
        private String description;
        private UUID assignedTo;
        private String dueDate;
        private TaskPriority priority;
    }

    @Data
    public static class CompleteTaskRequest {
        private String completionNote;
    }

    @Data
    public static class UpdateStatusRequest {
        private TaskStatus status;
    }
}
