package com.emplo.controller;

import com.emplo.entity.AuditLog;
import com.emplo.entity.Employee;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.repository.AuditLogRepository;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.UserRepository;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(value = "entity_type", required = false) String entityType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "actor_id", required = false) UUID actorId,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);

        List<AuditLog> logs = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Apply filters
        var filtered = logs.stream();
        if (entityType != null) filtered = filtered.filter(l -> entityType.equals(l.getEntityType()));
        if (action != null) filtered = filtered.filter(l -> action.equals(l.getAction()));
        if (actorId != null) filtered = filtered.filter(l -> actorId.equals(l.getActorId()));

        List<AuditLog> result = filtered.skip(offset).limit(limit).toList();

        // Build actor name cache
        Set<UUID> actorIds = new HashSet<>();
        result.forEach(l -> { if (l.getActorId() != null) actorIds.add(l.getActorId()); });
        Map<UUID, String> actorNames = new HashMap<>();
        if (!actorIds.isEmpty()) {
            userRepository.findAllById(actorIds).forEach(u -> {
                if (u.getEmployeeId() != null) {
                    employeeRepository.findById(u.getEmployeeId())
                            .ifPresent(emp -> actorNames.put(u.getId(), emp.getFullName()));
                }
                actorNames.putIfAbsent(u.getId(), u.getEmail());
            });
        }

        List<Map<String, Object>> response = result.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("actor_id", l.getActorId());
            m.put("actor_name", l.getActorId() != null ? actorNames.get(l.getActorId()) : null);
            m.put("action", l.getAction());
            m.put("entity_type", l.getEntityType());
            m.put("entity_id", l.getEntityId());
            m.put("changes", l.getChanges());
            m.put("before_data", l.getBeforeData());
            m.put("after_data", l.getAfterData());
            m.put("ip_address", l.getIpAddress());
            m.put("user_agent", l.getUserAgent());
            m.put("created_at", l.getCreatedAt());
            return m;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> listActions() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        List<String> actions = auditLogRepository.findAll().stream()
                .map(AuditLog::getAction).distinct().sorted().toList();
        return ResponseEntity.ok(actions);
    }

    @GetMapping("/entity-types")
    public ResponseEntity<List<String>> listEntityTypes() {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        List<String> types = auditLogRepository.findAll().stream()
                .map(AuditLog::getEntityType).distinct().sorted().toList();
        return ResponseEntity.ok(types);
    }

    /**
     * Get audit logs for a specific employee (by their user account's actor_id).
     * Useful for viewing "what did this person do?" or "what happened to this employee?".
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> employeeAudit(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);

        // Find the user account for this employee
        UUID actorUserId = userRepository.findByEmployeeId(employeeId)
                .map(User::getId).orElse(null);

        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        String empName = emp != null ? emp.getFullName() : "Unknown";

        List<AuditLog> allLogs = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Logs BY this employee (actions they performed)
        List<Map<String, Object>> actionsByEmployee = new ArrayList<>();
        // Logs ABOUT this employee (actions done to their record)
        List<Map<String, Object>> actionsOnEmployee = new ArrayList<>();

        for (AuditLog log : allLogs) {
            boolean isActor = actorUserId != null && actorUserId.equals(log.getActorId());
            boolean isTarget = employeeId.toString().equals(log.getEntityId())
                    && "employee".equals(log.getEntityType());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", log.getId());
            entry.put("action", log.getAction());
            entry.put("entity_type", log.getEntityType());
            entry.put("entity_id", log.getEntityId());
            entry.put("changes", log.getChanges());
            entry.put("ip_address", log.getIpAddress());
            entry.put("created_at", log.getCreatedAt());

            if (isActor) actionsByEmployee.add(entry);
            if (isTarget) actionsOnEmployee.add(entry);
        }

        // Categorize actions by type for summary
        Map<String, Long> categorySummary = new LinkedHashMap<>();
        for (Map<String, Object> log : actionsByEmployee) {
            String action = (String) log.get("action");
            categorySummary.merge(action, 1L, Long::sum);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employee_id", employeeId);
        response.put("employee_name", empName);
        response.put("actions_by_employee", actionsByEmployee.stream().limit(50).toList());
        response.put("actions_on_employee", actionsOnEmployee.stream().limit(50).toList());
        response.put("action_summary", categorySummary);
        response.put("total_actions_performed", actionsByEmployee.size());
        response.put("total_actions_received", actionsOnEmployee.size());

        return ResponseEntity.ok(response);
    }
}
