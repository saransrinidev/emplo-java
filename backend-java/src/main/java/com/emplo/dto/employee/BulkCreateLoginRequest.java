package com.emplo.dto.employee;

import com.emplo.entity.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateLoginRequest {

    @NotEmpty(message = "Employee IDs must not be empty")
    private List<UUID> employeeIds;

    @NotBlank(message = "Password is required")
    private String password;

    @Builder.Default
    private RoleName role = RoleName.employee;
}
