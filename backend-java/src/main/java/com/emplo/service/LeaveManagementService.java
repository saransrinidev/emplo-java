package com.emplo.service;

import com.emplo.entity.LeaveBalance;
import com.emplo.entity.LeaveTypeEntity;
import com.emplo.entity.User;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.LeaveBalanceRepository;
import com.emplo.repository.LeaveTypeEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveManagementService {

    private final LeaveTypeEntityRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public List<LeaveTypeEntity> listLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional
    public LeaveTypeEntity createLeaveType(String name, String code, BigDecimal defaultAnnualQuota,
                                            Boolean isPaid, Boolean carryForward) {
        if (leaveTypeRepository.findByCode(code).isPresent()) {
            throw new BadRequestException("Leave type code already exists");
        }
        if (leaveTypeRepository.findByName(name).isPresent()) {
            throw new BadRequestException("Leave type name already exists");
        }
        LeaveTypeEntity lt = LeaveTypeEntity.builder()
                .name(name)
                .code(code)
                .defaultAnnualQuota(defaultAnnualQuota != null ? defaultAnnualQuota : BigDecimal.ZERO)
                .isPaid(isPaid != null ? isPaid : true)
                .carryForward(carryForward != null ? carryForward : false)
                .build();
        return leaveTypeRepository.save(lt);
    }

    @Transactional
    public LeaveTypeEntity updateLeaveType(UUID id, String name, String code,
                                            BigDecimal defaultAnnualQuota, Boolean isPaid, Boolean carryForward) {
        LeaveTypeEntity lt = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave type not found"));
        if (name != null) lt.setName(name);
        if (code != null) lt.setCode(code);
        if (defaultAnnualQuota != null) lt.setDefaultAnnualQuota(defaultAnnualQuota);
        if (isPaid != null) lt.setIsPaid(isPaid);
        if (carryForward != null) lt.setCarryForward(carryForward);
        return leaveTypeRepository.save(lt);
    }

    @Transactional
    public void deleteLeaveType(UUID id) {
        LeaveTypeEntity lt = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave type not found"));
        leaveTypeRepository.delete(lt);
    }

    public List<LeaveBalance> getBalances(User user, UUID employeeId, Integer year) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return leaveBalanceRepository.findAllByEmployeeIdAndYear(target, targetYear);
    }

    @Transactional
    public LeaveBalance allocateBalance(UUID employeeId, UUID leaveTypeId, int year, BigDecimal allocated) {
        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new NotFoundException("Employee not found");
        }
        if (leaveTypeRepository.findById(leaveTypeId).isEmpty()) {
            throw new NotFoundException("Leave type not found");
        }

        LeaveBalance existing = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElse(null);

        if (existing != null) {
            existing.setAllocated(allocated);
            return leaveBalanceRepository.save(existing);
        }

        LeaveBalance bal = LeaveBalance.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .year(year)
                .allocated(allocated)
                .used(BigDecimal.ZERO)
                .pending(BigDecimal.ZERO)
                .build();
        return leaveBalanceRepository.save(bal);
    }
}
