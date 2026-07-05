package com.emplo.service;

import com.emplo.entity.*;
import com.emplo.entity.enums.EditRequestStatus;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditRequestService {

    private final EditAccessRequestRepository editAccessRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeEditPermissionRepository editPermissionRepository;
    private final EmployeeAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public EditAccessRequest createRequest(User user, EditableSection section, String reason) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        List<EditAccessRequest> existing = editAccessRequestRepository.findAllByEmployeeIdAndStatusIn(
                user.getEmployeeId(), List.of(EditRequestStatus.pending, EditRequestStatus.approved, EditRequestStatus.changes_submitted));
        boolean hasDuplicate = existing.stream().anyMatch(r -> r.getSection() == section);
        if (hasDuplicate) {
            throw new BadRequestException("You already have an active request for this section");
        }

        EditAccessRequest req = EditAccessRequest.builder()
                .employeeId(user.getEmployeeId())
                .section(section)
                .reason(reason)
                .status(EditRequestStatus.pending)
                .build();
        req = editAccessRequestRepository.save(req);

        Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        String empName = emp != null ? emp.getFullName() : "An employee";
        notificationService.notifyHrOnly(user, "Profile Edit Request",
                empName + " is requesting edit access to their " + section.name() + " section.");

        return req;
    }

    public List<EditAccessRequest> myRequests(User user) {
        if (user.getEmployeeId() == null) return List.of();
        List<EditAccessRequest> requests = editAccessRequestRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId());
        Instant now = Instant.now();
        requests.forEach(r -> {
            if (r.getStatus() == EditRequestStatus.approved && r.getWindowEnd() != null && now.isAfter(r.getWindowEnd())) {
                r.setStatus(EditRequestStatus.expired);
                editAccessRequestRepository.save(r);
            }
        });
        return requests;
    }

    public List<EditAccessRequest> pendingRequests() {
        return editAccessRequestRepository.findAllByStatusIn(
                List.of(EditRequestStatus.pending, EditRequestStatus.changes_submitted));
    }

    @Transactional
    public EditAccessRequest approveRequest(User user, UUID requestId, int windowHours, String remarks) {
        EditAccessRequest req = editAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (req.getStatus() != EditRequestStatus.pending) {
            throw new BadRequestException("Request is not in pending state");
        }
        if (windowHours < 1) {
            throw new BadRequestException("Window must be at least 1 hour");
        }

        Instant now = Instant.now();
        req.setStatus(EditRequestStatus.approved);
        req.setApprovedBy(user.getId());
        req.setWindowHours(windowHours);
        req.setWindowStart(now);
        req.setWindowEnd(now.plus(windowHours, ChronoUnit.HOURS));
        req.setHrRemarks(remarks);
        req.setPreviousData(getCurrentSectionData(req.getEmployeeId(), req.getSection()));

        EmployeeEditPermission perm = EmployeeEditPermission.builder()
                .employeeId(req.getEmployeeId())
                .section(req.getSection())
                .grantedBy(user.getId())
                .startAt(req.getWindowStart())
                .expiryAt(req.getWindowEnd())
                .isRevoked(false)
                .build();
        editPermissionRepository.save(perm);
        req = editAccessRequestRepository.save(req);

        final EditAccessRequest savedReq = req;
        userRepository.findByEmployeeId(savedReq.getEmployeeId()).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(), "Edit Access Approved",
                        "Your request to edit " + savedReq.getSection().name() + " has been approved. You have " + windowHours + " hours to make changes."));

        return req;
    }

    @Transactional
    public EditAccessRequest rejectRequest(User user, UUID requestId, String remarks) {
        EditAccessRequest req = editAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (req.getStatus() != EditRequestStatus.pending && req.getStatus() != EditRequestStatus.changes_submitted) {
            throw new BadRequestException("Cannot reject this request in its current state");
        }
        req.setStatus(EditRequestStatus.rejected);
        req.setHrRemarks(remarks);
        req.setConfirmedBy(user.getId());
        req.setConfirmedAt(Instant.now());
        req.setConfirmRemarks(remarks);
        req = editAccessRequestRepository.save(req);

        final EditAccessRequest savedReq = req;
        userRepository.findByEmployeeId(savedReq.getEmployeeId()).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(), "Edit Request Rejected",
                        "Your edit request for " + savedReq.getSection().name() + " was rejected. " + (remarks != null ? remarks : "")));

        return req;
    }

    @Transactional
    public EditAccessRequest submitChanges(User user, UUID requestId, Map<String, Object> data) {
        EditAccessRequest req = editAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (!req.getEmployeeId().equals(user.getEmployeeId())) {
            throw new ForbiddenException("Not your request");
        }
        if (req.getStatus() != EditRequestStatus.approved) {
            throw new BadRequestException("Request is not in approved state (edit window may have expired)");
        }
        Instant now = Instant.now();
        if (req.getWindowEnd() != null && now.isAfter(req.getWindowEnd())) {
            req.setStatus(EditRequestStatus.expired);
            editAccessRequestRepository.save(req);
            throw new BadRequestException("Edit window has expired");
        }

        req.setStatus(EditRequestStatus.changes_submitted);
        req.setSubmittedData(data);
        req.setSubmittedAt(now);
        req = editAccessRequestRepository.save(req);

        Employee emp = employeeRepository.findById(req.getEmployeeId()).orElse(null);
        String empName = emp != null ? emp.getFullName() : "An employee";
        notificationService.notifyHrOnly(user, "Profile Changes Submitted",
                empName + " has submitted their " + req.getSection().name() + " changes for confirmation.");

        return req;
    }

    @Transactional
    public EditAccessRequest confirmChanges(User user, UUID requestId, String action, String remarks) {
        EditAccessRequest req = editAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (req.getStatus() != EditRequestStatus.changes_submitted) {
            throw new BadRequestException("No changes to confirm");
        }

        if ("confirm".equals(action)) {
            req.setStatus(EditRequestStatus.confirmed);
        } else if ("reject".equals(action)) {
            req.setStatus(EditRequestStatus.rejected);
        } else {
            throw new BadRequestException("Action must be 'confirm' or 'reject'");
        }
        req.setConfirmedBy(user.getId());
        req.setConfirmedAt(Instant.now());
        req.setConfirmRemarks(remarks);
        req = editAccessRequestRepository.save(req);

        UUID empUserId = userRepository.findByEmployeeId(req.getEmployeeId()).map(User::getId).orElse(null);
        if (empUserId != null) {
            String msg = req.getStatus() == EditRequestStatus.confirmed
                    ? "Your " + req.getSection().name() + " changes have been confirmed and saved."
                    : "Your " + req.getSection().name() + " changes were rejected. " + (remarks != null ? remarks : "");
            notificationService.createNotification(empUserId, "Profile Changes Reviewed", msg);
        }

        return req;
    }

    private Map<String, Object> getCurrentSectionData(UUID employeeId, EditableSection section) {
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp == null) return Map.of();
        Map<String, Object> data = new HashMap<>();
        if (section == EditableSection.phone) {
            data.put("mobile_number", emp.getMobileNumber());
        } else if (section == EditableSection.address) {
            var addresses = addressRepository.findAllByEmployeeId(employeeId);
            data.put("addresses", addresses.stream().map(a -> Map.of(
                    "address_type", a.getAddressType().name(),
                    "address_line", a.getAddressLine() != null ? a.getAddressLine() : "",
                    "city", a.getCity() != null ? a.getCity() : "",
                    "state", a.getState() != null ? a.getState() : "",
                    "postal_code", a.getPostalCode() != null ? a.getPostalCode() : "",
                    "country", a.getCountry() != null ? a.getCountry() : ""
            )).toList());
        }
        return data;
    }
}
