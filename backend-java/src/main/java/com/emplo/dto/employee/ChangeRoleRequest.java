package com.emplo.dto.employee;

import com.emplo.entity.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    private RoleName role;
}
