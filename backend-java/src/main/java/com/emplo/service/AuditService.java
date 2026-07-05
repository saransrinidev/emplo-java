package com.emplo.service;

import com.emplo.entity.AuditLog;
import com.emplo.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(UUID actorId, String action, String entityType, String entityId,
                          Map<String, Object> changes) {
        logAction(actorId, action, entityType, entityId, changes, null, null);
    }

    public void logAction(UUID actorId, String action, String entityType, String entityId,
                          Map<String, Object> changes, Map<String, Object> beforeData, Map<String, Object> afterData) {
        String ipAddress = null;
        String userAgent = null;

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                ipAddress = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception ignored) {
        }

        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .changes(changes)
                .beforeData(beforeData)
                .afterData(afterData)
                .ipAddress(ipAddress)
                .userAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                .build();

        auditLogRepository.save(log);
    }
}
