package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.Policy;
import com.emplo.entity.PolicyAcknowledgement;
import com.emplo.entity.User;
import com.emplo.entity.enums.PolicyCategory;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.PolicyAcknowledgementRepository;
import com.emplo.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ─── List published policies (all employees) ──────────────────────────────

    @Cacheable(value = "policies", key = "'published_' + #category")
    public List<Policy> listPublished(PolicyCategory category) {
        if (category != null) {
            return policyRepository.findAllByCategoryAndIsPublishedTrueOrderByCreatedAtDesc(category);
        }
        return policyRepository.findAllByIsPublishedTrueOrderByCreatedAtDesc();
    }

    // ─── HR: list all (including unpublished) ─────────────────────────────────

    public List<Policy> listAll() {
        return policyRepository.findAllByOrderByCreatedAtDesc();
    }

    public Policy get(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy not found"));
    }

    // ─── HR: create ────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public Policy create(User user, String title, PolicyCategory category, String content,
                         String attachmentUrl, Boolean requiresAcknowledgement, Boolean isPublished) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Content is required");
        }

        Policy policy = Policy.builder()
                .title(title.trim())
                .category(category != null ? category : PolicyCategory.general)
                .content(content)
                .attachmentUrl(attachmentUrl)
                .version(1)
                .requiresAcknowledgement(requiresAcknowledgement != null ? requiresAcknowledgement : false)
                .isPublished(isPublished != null ? isPublished : true)
                .createdBy(user.getId())
                .build();

        policy = policyRepository.save(policy);

        auditService.logAction(user.getId(), "create_policy", "policy",
                policy.getId().toString(), Map.of("title", policy.getTitle()));

        if (policy.getIsPublished()) {
            notifyAllEmployees("New Policy Published: " + policy.getTitle(),
                    "A new " + humanize(policy.getCategory().name()) + " policy has been published"
                            + (policy.getRequiresAcknowledgement() ? ". Please review and acknowledge it." : "."));
        }

        return policy;
    }

    // ─── HR: update (bumps version if content changes) ────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public Policy update(User user, UUID id, String title, PolicyCategory category, String content,
                         String attachmentUrl, Boolean requiresAcknowledgement, Boolean isPublished) {
        Policy policy = get(id);

        boolean contentChanged = content != null && !content.equals(policy.getContent());

        if (title != null) policy.setTitle(title.trim());
        if (category != null) policy.setCategory(category);
        if (content != null) policy.setContent(content);
        if (attachmentUrl != null) policy.setAttachmentUrl(attachmentUrl);
        if (requiresAcknowledgement != null) policy.setRequiresAcknowledgement(requiresAcknowledgement);
        boolean wasPublished = policy.getIsPublished();
        if (isPublished != null) policy.setIsPublished(isPublished);

        if (contentChanged) {
            policy.setVersion(policy.getVersion() + 1);
        }

        policy = policyRepository.save(policy);

        auditService.logAction(user.getId(), "update_policy", "policy",
                policy.getId().toString(), Map.of("title", policy.getTitle(), "version", policy.getVersion().toString()));

        if (contentChanged && policy.getIsPublished()) {
            notifyAllEmployees("Policy Updated: " + policy.getTitle(),
                    "The " + humanize(policy.getCategory().name()) + " policy has been updated to version "
                            + policy.getVersion() + ". Please review the changes.");
        } else if (!wasPublished && Boolean.TRUE.equals(policy.getIsPublished())) {
            notifyAllEmployees("New Policy Published: " + policy.getTitle(),
                    "A new " + humanize(policy.getCategory().name()) + " policy has been published.");
        }

        return policy;
    }

    // ─── HR: delete ─────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public void delete(UUID id) {
        Policy policy = get(id);
        policyRepository.delete(policy);
    }

    // ─── Employee: acknowledge a policy ───────────────────────────────────────

    @Transactional
    public void acknowledge(User user, UUID policyId) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked to this account");
        }
        Policy policy = get(policyId);

        var existing = acknowledgementRepository.findByPolicyIdAndEmployeeId(policyId, user.getEmployeeId());
        if (existing.isPresent()) {
            // Already acknowledged this version or an older one; update to current version
            PolicyAcknowledgement ack = existing.get();
            ack.setPolicyVersion(policy.getVersion());
            acknowledgementRepository.save(ack);
            return;
        }

        PolicyAcknowledgement ack = PolicyAcknowledgement.builder()
                .policyId(policyId)
                .employeeId(user.getEmployeeId())
                .policyVersion(policy.getVersion())
                .build();
        acknowledgementRepository.save(ack);
    }

    // ─── Employee: which published policies still need acknowledgement ───────

    public List<Policy> myPendingAcknowledgements(User user) {
        if (user.getEmployeeId() == null) return List.of();

        List<Policy> published = policyRepository.findAllByIsPublishedTrueOrderByCreatedAtDesc().stream()
                .filter(Policy::getRequiresAcknowledgement)
                .toList();

        List<PolicyAcknowledgement> myAcks = acknowledgementRepository.findAllByEmployeeId(user.getEmployeeId());
        Map<UUID, Integer> ackedVersionByPolicy = new HashMap<>();
        for (PolicyAcknowledgement ack : myAcks) {
            ackedVersionByPolicy.put(ack.getPolicyId(), ack.getPolicyVersion());
        }

        return published.stream()
                .filter(p -> {
                    Integer ackedVersion = ackedVersionByPolicy.get(p.getId());
                    return ackedVersion == null || ackedVersion < p.getVersion();
                })
                .toList();
    }

    // ─── HR: acknowledgement stats for a policy ───────────────────────────────

    public Map<String, Object> acknowledgementStats(UUID policyId) {
        Policy policy = get(policyId);
        long totalEmployees = employeeRepository.count();
        long acknowledgedCount = acknowledgementRepository.findAllByPolicyId(policyId).stream()
                .filter(a -> a.getPolicyVersion() >= policy.getVersion())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_employees", totalEmployees);
        stats.put("acknowledged_count", acknowledgedCount);
        stats.put("pending_count", Math.max(0, totalEmployees - acknowledgedCount));
        stats.put("percentage", totalEmployees > 0 ? Math.round((double) acknowledgedCount / totalEmployees * 100) : 0);
        return stats;
    }

    public boolean hasAcknowledged(User user, UUID policyId) {
        if (user.getEmployeeId() == null) return false;
        Policy policy = get(policyId);
        return acknowledgementRepository.findByPolicyIdAndEmployeeId(policyId, user.getEmployeeId())
                .map(a -> a.getPolicyVersion() >= policy.getVersion())
                .orElse(false);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void notifyAllEmployees(String title, String message) {
        List<Employee> all = employeeRepository.findAll();
        for (Employee emp : all) {
            notificationService.notifyEmployeeById(emp.getId(), title, message);
        }
    }

    private String humanize(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
