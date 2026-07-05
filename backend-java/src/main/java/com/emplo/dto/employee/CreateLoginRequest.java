package com.emplo.dto.employee;

import com.emplo.entity.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLoginRequest {

    @NotBlank(message = "Password is required")
    private String password;

    @Builder.Default
    private RoleName role = RoleName.employee;
}
