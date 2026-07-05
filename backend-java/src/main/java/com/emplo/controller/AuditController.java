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
}
