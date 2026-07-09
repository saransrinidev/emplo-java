package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.OnboardingTask;
import com.emplo.entity.OnboardingTemplate;
import com.emplo.entity.User;
import com.emplo.entity.enums.OnboardingCategory;
import com.emplo.entity.enums.OnboardingTaskStatus;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.OnboardingTaskRepository;
import com.emplo.repository.OnboardingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    // ─── Templates (HR manages these) ─────────────────────────────────────────

    @Cacheable("onboarding_templates")
    public List<OnboardingTemplate> listTemplates() {
        return templateRepository.findAllByIsActiveTrueOrderBySortOrder();
    }

    public List<OnboardingTemplate> listAllTemplates() {
        return templateRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "onboarding_templates", allEntries = true)
    public OnboardingTemplate createTemplate(String title, String description, OnboardingCategory category,
                                             Integer sortOrder, Boolean isRequired, Integer dueDays,
                                             String actionType, String actionUrl) {
        OnboardingTemplate template = OnboardingTemplate.builder()
                .title(title)
                .description(description)
                .category(category)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isRequired(isRequired != null ? isRequired : true)
                .dueDays(dueDays)
                .actionType(actionType)
                .actionUrl(actionUrl)
                .isActive(true)
                .build();
        return templateRepository.save(template);
    }

    @Transactional
    @CacheEvict(value = "onboarding_templates", allEntries = true)
    public OnboardingTemplate updateTemplate(UUID templateId, String title, String description,
                                             OnboardingCategory category, Integer sortOrder,
                                             Boolean isRequired, Integer dueDays, String actionType,
                                             String actionUrl, Boolean isActive) {
        OnboardingTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        if (title != null) template.setTitle(title);
        if (description != null) template.setDescription(description);
        if (category != null) template.setCategory(category);
        if (sortOrder != null) template.setSortOrder(sortOrder);
        if (isRequired != null) template.setIsRequired(isRequired);
        if (dueDays != null) template.setDueDays(dueDays);
        if (actionType != null) template.setActionType(actionType);
        if (actionUrl != null) template.setActionUrl(actionUrl);
        if (isActive != null) template.setIsActive(isActive);
        return templateRepository.save(template);
    }

    @Transactional
    @CacheEvict(value = "onboarding_templates", allEntries = true)
    public void deleteTemplate(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new NotFoundException("Template not found");
        }
        templateRepository.deleteById(templateId);
    }

    // ─── Assign onboarding to employee ────────────────────────────────────────

    @Transactional
    public List<OnboardingTask> initializeOnboarding(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        // Don't re-initialize if already done
        if (taskRepository.existsByEmployeeId(employeeId)) {
            throw new BadRequestException("Onboarding already initialized for this employee");
        }

        List<OnboardingTemplate> templates = templateRepository.findAllByIsActiveTrueOrderBySortOrder();
        if (templates.isEmpty()) {
            return List.of(); // No templates yet, return empty
        }

        LocalDate joinDate = emp.getDateOfJoining() != null ? emp.getDateOfJoining() : LocalDate.now();

        List<OnboardingTask> tasks = new ArrayList<>();
        for (OnboardingTemplate t : templates) {
            LocalDate dueDate = t.getDueDays() != null ? joinDate.plusDays(t.getDueDays()) : null;

            OnboardingTask task = OnboardingTask.builder()
                    .employeeId(employeeId)
                    .templateId(t.getId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .category(t.getCategory())
                    .sortOrder(t.getSortOrder())
                    .isRequired(t.getIsRequired())
                    .status(OnboardingTaskStatus.pending)
                    .dueDate(dueDate)
                    .actionType(t.getActionType())
                    .actionUrl(t.getActionUrl())
                    .build();
            tasks.add(task);
        }

        return taskRepository.saveAll(tasks);
    }

    // ─── Employee: get my onboarding tasks ────────────────────────────────────

    public List<OnboardingTask> getMyTasks(User user) {
        if (user.getEmployeeId() == null) return List.of();
        return taskRepository.findAllByEmployeeIdOrderBySortOrder(user.getEmployeeId());
    }

    // ─── Employee: get onboarding progress ────────────────────────────────────

    public Map<String, Object> getMyProgress(User user) {
        if (user.getEmployeeId() == null) {
            return Map.of("total", 0, "completed", 0, "percentage", 0, "is_onboarding", false);
        }
        long total = taskRepository.countByEmployeeId(user.getEmployeeId());
        if (total == 0) {
            return Map.of("total", 0, "completed", 0, "percentage", 0, "is_onboarding", false);
        }
        long completed = taskRepository.countByEmployeeIdAndStatus(user.getEmployeeId(), OnboardingTaskStatus.completed);
        long skipped = taskRepository.countByEmployeeIdAndStatus(user.getEmployeeId(), OnboardingTaskStatus.skipped);
        int percentage = (int) Math.round((double) (completed + skipped) / total * 100);
        boolean allDone = percentage >= 100;

        return Map.of(
                "total", total,
                "completed", completed + skipped,
                "percentage", percentage,
                "is_onboarding", !allDone
        );
    }

    // ─── Employee: complete a task ────────────────────────────────────────────

    @Transactional
    public OnboardingTask completeTask(User user, UUID taskId, String notes) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Onboarding task not found"));

        // Verify ownership
        if (!task.getEmployeeId().equals(user.getEmployeeId()) &&
                user.getRole().getName() != RoleName.hr_admin) {
            throw new BadRequestException("Not your task");
        }

        if (task.getStatus() == OnboardingTaskStatus.completed) {
            throw new BadRequestException("Task already completed");
        }

        task.setStatus(OnboardingTaskStatus.completed);
        task.setCompletedAt(Instant.now());
        task.setCompletedBy(user.getId());
        if (notes != null && !notes.isBlank()) {
            task.setNotes(notes);
        }

        task = taskRepository.save(task);

        // Check if onboarding is now complete
        long total = taskRepository.countByEmployeeId(task.getEmployeeId());
        long done = taskRepository.countByEmployeeIdAndStatus(task.getEmployeeId(), OnboardingTaskStatus.completed)
                + taskRepository.countByEmployeeIdAndStatus(task.getEmployeeId(), OnboardingTaskStatus.skipped);

        if (done >= total) {
            // Notify HR that employee completed onboarding
            notificationService.notifyHrOnly(user, "Onboarding Completed",
                    employeeRepository.findById(task.getEmployeeId())
                            .map(e -> e.getFullName() + " has completed all onboarding tasks.")
                            .orElse("An employee completed onboarding."));
        }

        return task;
    }

    // ─── Employee: skip a task (only non-required) ────────────────────────────

    @Transactional
    public OnboardingTask skipTask(User user, UUID taskId) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Onboarding task not found"));

        if (!task.getEmployeeId().equals(user.getEmployeeId()) &&
                user.getRole().getName() != RoleName.hr_admin) {
            throw new BadRequestException("Not your task");
        }

        if (task.getIsRequired()) {
            throw new BadRequestException("Cannot skip a required task");
        }

        task.setStatus(OnboardingTaskStatus.skipped);
        task.setCompletedAt(Instant.now());
        task.setCompletedBy(user.getId());
        return taskRepository.save(task);
    }

    // ─── HR: View onboarding status for an employee ───────────────────────────

    public List<OnboardingTask> getTasksForEmployee(UUID employeeId) {
        return taskRepository.findAllByEmployeeIdOrderBySortOrder(employeeId);
    }

    // ─── HR: get onboarding summary across all new employees ──────────────────

    public List<Map<String, Object>> getOnboardingSummary() {
        // Find all employees who have onboarding tasks
        List<OnboardingTask> allTasks = taskRepository.findAll();
        Map<UUID, List<OnboardingTask>> byEmployee = allTasks.stream()
                .collect(Collectors.groupingBy(OnboardingTask::getEmployeeId));

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map.Entry<UUID, List<OnboardingTask>> entry : byEmployee.entrySet()) {
            UUID empId = entry.getKey();
            List<OnboardingTask> tasks = entry.getValue();
            long total = tasks.size();
            long completed = tasks.stream().filter(t ->
                    t.getStatus() == OnboardingTaskStatus.completed || t.getStatus() == OnboardingTaskStatus.skipped).count();
            int percentage = (int) Math.round((double) completed / total * 100);

            Employee emp = employeeRepository.findById(empId).orElse(null);
            Map<String, Object> item = new HashMap<>();
            item.put("employee_id", empId.toString());
            item.put("employee_name", emp != null ? emp.getFullName() : "Unknown");
            item.put("employee_code", emp != null ? emp.getEmployeeCode() : null);
            item.put("department", emp != null ? emp.getDepartment() : null);
            item.put("total_tasks", total);
            item.put("completed_tasks", completed);
            item.put("percentage", percentage);
            item.put("is_complete", percentage >= 100);
            item.put("date_of_joining", emp != null && emp.getDateOfJoining() != null ? emp.getDateOfJoining().toString() : null);
            summary.add(item);
        }

        // Sort: incomplete first, then by percentage ascending
        summary.sort((a, b) -> {
            boolean aComplete = (boolean) a.get("is_complete");
            boolean bComplete = (boolean) b.get("is_complete");
            if (aComplete != bComplete) return aComplete ? 1 : -1;
            return Integer.compare((int) a.get("percentage"), (int) b.get("percentage"));
        });

        return summary;
    }
}
