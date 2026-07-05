package com.emplo.dto.employee;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreateRequest {

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    private String email;

    private String mobileNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private LocalDate dateOfJoining;
    private String department;
    private String designation;
    private UUID managerId;
    private String employmentStatus;
    private String workLocation;
    private BigDecimal initialSalary;
}
