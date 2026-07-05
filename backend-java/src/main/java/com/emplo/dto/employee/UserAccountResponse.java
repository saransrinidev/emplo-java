package com.emplo.dto.employee;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountResponse {

    private String id;
    private String email;
    private String role;
    private String employeeId;
}
