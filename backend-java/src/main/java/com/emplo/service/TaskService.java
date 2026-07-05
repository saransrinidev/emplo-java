package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.Task;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.TaskPriority;
import com.emplo.entity.enums.TaskStatus;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.TaskRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Task createTask(User user, String title, String description, UUID assignedTo,
                           String dueDate, TaskPriority priority) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("Title is required");
        }
        Employee target = employeeRepository.findById(assignedTo)
                .orElseThrow(() -> new NotFoundException("Target employee not found"));
        if (user.getRole().getName() == RoleName.manager && !user.getEmployeeId().equals(target.getManagerId())) {
            throw new ForbiddenException("You can only assign tasks to your direct reports");
        }

        LocalDate due = null;
        if (dueDate != null && !dueDate.isEmpty()) {
            try {
                due = LocalDate.parse(dueDate);
            } catch (Exception e) {
                throw new BadRequestException("Invalid due_date format (use YYYY-MM-DD)");
            }
        }

        Task task = Task.builder()
                .title(title.trim())
                .description(description)
                .assignedBy(user.getEmployeeId())
                .assignedTo(assignedTo)
                .dueDate(due)
                .priority(priority != null ? priority : TaskPriority.medium)
                .status(TaskStatus.open)
                .build();
        task = taskRepository.save(task);

        LocalDate finalDue = due;
        userRepository.findByEmployeeId(assignedTo).ifPresent(empUser -> {
            Employee assigner = employeeRepository.findById(user.getEmployeeId()).orElse(null);
            String msg = (assigner != null ? assigner.getFullName() : "Manager") + " assigned you: " + title.trim()
                    + (finalDue != null ? " (Due: " + finalDue + ")" : "");
            notificationService.createNotification(empUser.getId(), "New Task Assigned", msg);
        });

        return task;
    }

    public List<Task> myTasks(User user, TaskStatus status) {
        if (user.getEmployeeId() == null) return List.of();
        List<Task> tasks = taskRepository.findAllByAssignedToOrderByCreatedAtDesc(user.getEmployeeId());
        if (status != null) {
            tasks = tasks.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tasks;
    }

    public List<Task> assignedTasks(User user, TaskStatus status) {
        if (user.getEmployeeId() == null) return List.of();
        List<Task> tasks = taskRepository.findAllByAssignedByOrderByCreatedAtDesc(user.getEmployeeId());
        if (status != null) {
            tasks = tasks.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tasks;
    }

    @Transactional
    public Task completeTask(User user, UUID taskId, String completionNote) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        if (!task.getAssignedTo().equals(user.getEmployeeId())) {
            throw new ForbiddenException("This task is not assigned to you");
        }
        if (task.getStatus() == TaskStatus.completed || task.getStatus() == TaskStatus.closed) {
            throw new BadRequestException("Task is already completed/closed");
        }

        task.setStatus(TaskStatus.completed);
        task.setCompletionNote(completionNote);
        task.setCompletedAt(Instant.now());
        task = taskRepository.save(task);

        final Task savedTask = task;
        userRepository.findByEmployeeId(savedTask.getAssignedBy()).ifPresent(mgrUser -> {
            Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
            notificationService.createNotification(mgrUser.getId(), "Task Completed",
                    (emp != null ? emp.getFullName() : "Employee") + " completed: " + savedTask.getTitle());
        });

        return task;
    }

    @Transactional
    public Task updateStatus(User user, UUID taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        if (!task.getAssignedBy().equals(user.getEmployeeId()) && user.getRole().getName() != RoleName.hr_admin) {
            throw new ForbiddenException("You didn't assign this task");
        }
        task.setStatus(status);
        if (status == TaskStatus.closed && task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now());
        }
        return taskRepository.save(task);
    }
}
