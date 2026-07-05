package com.emplo.dto.employee;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportItem {

    private String employeeCode;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String dateOfJoining;
    private String department;
    private String designation;
    private String employmentStatus;
    private String workLocation;
    private String initialSalary;
}
