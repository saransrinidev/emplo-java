package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.SalaryStructure;
import com.emplo.entity.User;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final AuthorizationService authorizationService;

    private static final Map<String, Object> DEFAULT_EARNINGS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("basic_pay", 0), Map.entry("hra", 0), Map.entry("dearness_allowance", 0),
            Map.entry("special_allowance", 0), Map.entry("conveyance_allowance", 0),
            Map.entry("medical_allowance", 0), Map.entry("internet_allowance", 0),
            Map.entry("telephone_allowance", 0), Map.entry("food_allowance", 0),
            Map.entry("shift_allowance", 0), Map.entry("performance_bonus", 0),
            Map.entry("incentives", 0), Map.entry("overtime", 0), Map.entry("other_allowances", 0)
    ));

    private static final Map<String, Object> DEFAULT_DEDUCTIONS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("employee_pf", 0), Map.entry("employee_esi", 0), Map.entry("professional_tax", 0),
            Map.entry("income_tax_tds", 0), Map.entry("labour_welfare_fund", 0),
            Map.entry("nps_employee", 0), Map.entry("insurance_deduction", 0),
            Map.entry("loan_recovery", 0), Map.entry("advance_recovery", 0), Map.entry("other_deductions", 0)
    ));

    private static final Map<String, Object> DEFAULT_EMPLOYER_CONTRIBUTIONS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("employer_pf", 0), Map.entry("employer_esi", 0), Map.entry("gratuity", 0),
            Map.entry("employer_insurance", 0), Map.entry("employer_nps", 0)
    ));

    public SalaryStructure getMySalaryStructure(User user) {
        if (user.getEmployeeId() == null) return null;
        return salaryStructureRepository.findByEmployeeId(user.getEmployeeId()).orElse(null);
    }

    public SalaryStructure getSalaryStructure(User user, UUID employeeId) {
        authorizationService.requireViewEmployee(user, employeeId);
        return salaryStructureRepository.findByEmployeeId(employeeId).orElse(null);
    }

    public Map<String, Object> getDefaultTemplate() {
        return Map.of(
                "earnings", DEFAULT_EARNINGS,
                "deductions", DEFAULT_DEDUCTIONS,
                "employer_contributions", DEFAULT_EMPLOYER_CONTRIBUTIONS
        );
    }

    @Transactional
    public SalaryStructure upsert(UUID employeeId, Map<String, Object> earnings,
                                   Map<String, Object> deductions, Map<String, Object> employerContributions,
                                   String effectiveDate) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        Map<String, BigDecimal> totals = calculateTotals(earnings, deductions, employerContributions);
        if (totals.get("netSalary").compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Net salary cannot be negative. Reduce deductions.");
        }

        LocalDate effective = effectiveDate != null ? LocalDate.parse(effectiveDate) : LocalDate.now();

        SalaryStructure existing = salaryStructureRepository.findByEmployeeId(employeeId).orElse(null);
        if (existing != null) {
            existing.setEarnings(earnings);
            existing.setDeductions(deductions);
            existing.setEmployerContributions(employerContributions);
            existing.setEffectiveDate(effective);
            existing.setGrossSalary(totals.get("grossSalary"));
            existing.setTotalDeductions(totals.get("totalDeductions"));
            existing.setNetSalary(totals.get("netSalary"));
            existing.setEmployerCost(totals.get("employerCost"));
            existing.setMonthlyCtc(totals.get("monthlyCtc"));
            existing.setAnnualCtc(totals.get("annualCtc"));
            return salaryStructureRepository.save(existing);
        }

        SalaryStructure structure = SalaryStructure.builder()
                .employeeId(employeeId)
                .effectiveDate(effective)
                .earnings(earnings)
                .deductions(deductions)
                .employerContributions(employerContributions)
                .grossSalary(totals.get("grossSalary"))
                .totalDeductions(totals.get("totalDeductions"))
                .netSalary(totals.get("netSalary"))
                .employerCost(totals.get("employerCost"))
                .monthlyCtc(totals.get("monthlyCtc"))
                .annualCtc(totals.get("annualCtc"))
                .build();
        return salaryStructureRepository.save(structure);
    }

    public Map<String, BigDecimal> calculateTotals(Map<String, Object> earnings,
                                                    Map<String, Object> deductions,
                                                    Map<String, Object> employerContributions) {
        BigDecimal gross = sumValues(earnings);
        BigDecimal totalDed = sumValues(deductions);
        BigDecimal empCost = sumValues(employerContributions);
        BigDecimal net = gross.subtract(totalDed).max(BigDecimal.ZERO);
        BigDecimal monthlyCtc = gross.add(empCost);
        BigDecimal annualCtc = monthlyCtc.multiply(BigDecimal.valueOf(12));

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("grossSalary", gross.setScale(2, RoundingMode.HALF_UP));
        result.put("totalDeductions", totalDed.setScale(2, RoundingMode.HALF_UP));
        result.put("netSalary", net.setScale(2, RoundingMode.HALF_UP));
        result.put("employerCost", empCost.setScale(2, RoundingMode.HALF_UP));
        result.put("monthlyCtc", monthlyCtc.setScale(2, RoundingMode.HALF_UP));
        result.put("annualCtc", annualCtc.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private BigDecimal sumValues(Map<String, Object> map) {
        if (map == null) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (Object v : map.values()) {
            if (v != null) {
                try {
                    sum = sum.add(new BigDecimal(v.toString()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return sum;
    }
}
