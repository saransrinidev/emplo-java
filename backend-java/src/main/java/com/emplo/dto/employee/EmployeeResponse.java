package com.emplo.dto.employee;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private UUID id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String mobileNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String profilePhoto;
    private LocalDate dateOfJoining;
    private String department;
    private String designation;
    private UUID managerId;
    private String employmentStatus;
    private String workLocation;
    private Instant createdAt;
    private Instant updatedAt;
}
