package com.emplo.dto.employee;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeUpdateRequest {

    private String employeeCode;
    private String fullName;
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
    private String profilePhoto;
}
