package com.emplo.dto.employee;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWithRoleResponse {

    private UUID id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String department;
    private String designation;
    private String employmentStatus;
    private String workLocation;
    private UUID managerId;
    private String role;
    private String profilePhoto;
}
