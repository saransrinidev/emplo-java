package com.emplo.controller;

import com.emplo.entity.EmployeeBankAccount;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.BankAccountService;
import com.emplo.service.EncryptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final EncryptionService encryptionService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        List<EmployeeBankAccount> accounts = bankAccountService.list(user, employeeId);
        List<Map<String, Object>> masked = accounts.stream().map(a -> Map.<String, Object>of(
                "id", a.getId(),
                "employee_id", a.getEmployeeId(),
                "account_holder_name", a.getAccountHolderName(),
                "bank_name", a.getBankName(),
                "account_number_masked", encryptionService.maskAccountNumber(a.getAccountNumberEnc()),
                "ifsc_swift_code", a.getIfscSwiftCode(),
                "branch", a.getBranch() != null ? a.getBranch() : "",
                "is_primary", a.getIsPrimary(),
                "created_at", a.getCreatedAt().toString(),
                "updated_at", a.getUpdatedAt().toString()
        )).toList();
        return ResponseEntity.ok(masked);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@RequestBody BankAccountRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        EmployeeBankAccount acct = bankAccountService.add(request.getEmployeeId(),
                request.getAccountHolderName(), request.getBankName(),
                request.getAccountNumber(), request.getIfscSwiftCode(),
                request.getBranch(), request.getIsPrimary());
        return ResponseEntity.status(HttpStatus.CREATED).body(maskedResponse(acct));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id, @RequestBody BankAccountRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        EmployeeBankAccount acct = bankAccountService.update(id,
                request.getAccountHolderName(), request.getBankName(),
                request.getAccountNumber(), request.getIfscSwiftCode(),
                request.getBranch(), request.getIsPrimary());
        return ResponseEntity.ok(maskedResponse(acct));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        bankAccountService.delete(id);
    }

    private Map<String, Object> maskedResponse(EmployeeBankAccount a) {
        return Map.of(
                "id", a.getId(),
                "employee_id", a.getEmployeeId(),
                "account_holder_name", a.getAccountHolderName(),
                "bank_name", a.getBankName(),
                "account_number_masked", encryptionService.maskAccountNumber(a.getAccountNumberEnc()),
                "ifsc_swift_code", a.getIfscSwiftCode(),
                "branch", a.getBranch() != null ? a.getBranch() : "",
                "is_primary", a.getIsPrimary(),
                "created_at", a.getCreatedAt().toString(),
                "updated_at", a.getUpdatedAt().toString()
        );
    }

    @Data
    public static class BankAccountRequest {
        private UUID employeeId;
        private String accountHolderName;
        private String bankName;
        private String accountNumber;
        private String ifscSwiftCode;
        private String branch;
        private Boolean isPrimary;
    }
}
