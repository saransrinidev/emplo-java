package com.emplo.controller;

import com.emplo.entity.LeaveBalance;
import com.emplo.entity.LeaveTypeEntity;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.LeaveManagementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leave-management")
@RequiredArgsConstructor
public class LeaveManagementController {

    private final LeaveManagementService leaveManagementService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeEntity>> listTypes() {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(leaveManagementService.listLeaveTypes());
    }

    @PostMapping("/types")
    public ResponseEntity<LeaveTypeEntity> createType(@RequestBody LeaveTypeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveManagementService.createLeaveType(request.getName(), request.getCode(),
                        request.getDefaultAnnualQuota(), request.getIsPaid(), request.getCarryForward()));
    }

    @PutMapping("/types/{id}")
    public ResponseEntity<LeaveTypeEntity> updateType(@PathVariable UUID id, @RequestBody LeaveTypeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(leaveManagementService.updateLeaveType(id, request.getName(), request.getCode(),
                request.getDefaultAnnualQuota(), request.getIsPaid(), request.getCarryForward()));
    }

    @DeleteMapping("/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteType(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        leaveManagementService.deleteLeaveType(id);
    }

    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalance>> getBalances(
            @RequestParam(value = "employee_id", required = false) UUID employeeId,
            @RequestParam(value = "year", required = false) Integer year) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(leaveManagementService.getBalances(user, employeeId, year));
    }

    @PostMapping("/balances")
    public ResponseEntity<LeaveBalance> allocate(@RequestBody AllocateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveManagementService.allocateBalance(request.getEmployeeId(),
                        request.getLeaveTypeId(), request.getYear(), request.getAllocated()));
    }

    @Data
    public static class LeaveTypeRequest {
        private String name;
        private String code;
        private BigDecimal defaultAnnualQuota;
        private Boolean isPaid;
        private Boolean carryForward;
    }

    @Data
    public static class AllocateRequest {
        private UUID employeeId;
        private UUID leaveTypeId;
        private int year;
        private BigDecimal allocated;
    }
}
