package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.ProfileChangeRequest;
import com.emplo.entity.User;
import com.emplo.entity.enums.ChangeStatus;
import com.emplo.entity.enums.EditableSection;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.ProfileChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileChangeService {

    private final ProfileChangeRequestRepository changeRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public ProfileChangeRequest submitChange(User user, EditableSection section, Map<String, Object> proposedChanges) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        Map<String, Object> previous = new HashMap<>();
        if (section == EditableSection.phone || section == EditableSection.address) {
            previous.put("mobile_number", emp.getMobileNumber());
        }

        ProfileChangeRequest req = ProfileChangeRequest.builder()
                .employeeId(user.getEmployeeId())
                .section(section)
                .proposedChanges(proposedChanges)
                .previousValues(previous)
                .status(ChangeStatus.pending)
                .build();
        return changeRequestRepository.save(req);
    }

    public List<ProfileChangeRequest> myChanges(User user) {
        if (user.getEmployeeId() == null) return List.of();
        return changeRequestRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId());
    }

    public List<ProfileChangeRequest> pendingChanges() {
        return changeRequestRepository.findAllByStatusOrderByCreatedAtAsc(ChangeStatus.pending);
    }

    @Transactional
    public ProfileChangeRequest reviewChange(User user, UUID requestId, String action, String remarks) {
        ProfileChangeRequest req = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Change request not found"));
        if (req.getStatus() != ChangeStatus.pending) {
            throw new BadRequestException("Request is not pending");
        }
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BadRequestException("Action must be 'approve' or 'reject'");
        }

        if ("approve".equals(action)) {
            req.setStatus(ChangeStatus.approved);
            // Apply changes to employee
            Employee emp = employeeRepository.findById(req.getEmployeeId()).orElse(null);
            if (emp != null && req.getProposedChanges() != null) {
                req.getProposedChanges().forEach((field, value) -> {
                    if ("mobile_number".equals(field) || "mobileNumber".equals(field)) {
                        emp.setMobileNumber(value != null ? value.toString() : null);
                    }
                });
                employeeRepository.save(emp);
            }
        } else {
            req.setStatus(ChangeStatus.rejected);
        }
        req.setReviewedBy(user.getId());
        req.setReviewRemarks(remarks);
        req.setReviewedAt(Instant.now());
        return changeRequestRepository.save(req);
    }
}
