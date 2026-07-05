package com.emplo.service;

import com.emplo.entity.EmployeeBankAccount;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeBankAccountRepository;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final EmployeeBankAccountRepository bankAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final EncryptionService encryptionService;

    public List<EmployeeBankAccount> list(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        if (user.getRole().getName() == RoleName.employee && !target.equals(user.getEmployeeId())) {
            throw new ForbiddenException("Cannot view another employee's bank accounts");
        }
        return bankAccountRepository.findAllByEmployeeId(target);
    }

    @Transactional
    public EmployeeBankAccount add(UUID employeeId, String accountHolderName, String bankName,
                                    String accountNumber, String ifscSwiftCode, String branch, Boolean isPrimary) {
        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new NotFoundException("Employee not found");
        }
        EmployeeBankAccount acct = EmployeeBankAccount.builder()
                .employeeId(employeeId)
                .accountHolderName(accountHolderName)
                .bankName(bankName)
                .accountNumberEnc(encryptionService.encrypt(accountNumber))
                .ifscSwiftCode(ifscSwiftCode)
                .branch(branch)
                .isPrimary(isPrimary != null ? isPrimary : true)
                .build();
        return bankAccountRepository.save(acct);
    }

    @Transactional
    public EmployeeBankAccount update(UUID acctId, String accountHolderName, String bankName,
                                       String accountNumber, String ifscSwiftCode, String branch, Boolean isPrimary) {
        EmployeeBankAccount acct = bankAccountRepository.findById(acctId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));
        if (accountHolderName != null) acct.setAccountHolderName(accountHolderName);
        if (bankName != null) acct.setBankName(bankName);
        if (accountNumber != null) acct.setAccountNumberEnc(encryptionService.encrypt(accountNumber));
        if (ifscSwiftCode != null) acct.setIfscSwiftCode(ifscSwiftCode);
        if (branch != null) acct.setBranch(branch);
        if (isPrimary != null) acct.setIsPrimary(isPrimary);
        return bankAccountRepository.save(acct);
    }

    @Transactional
    public void delete(UUID acctId) {
        EmployeeBankAccount acct = bankAccountRepository.findById(acctId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));
        bankAccountRepository.delete(acct);
    }

    public String maskAccountNumber(EmployeeBankAccount acct) {
        return encryptionService.maskAccountNumber(acct.getAccountNumberEnc());
    }
}
