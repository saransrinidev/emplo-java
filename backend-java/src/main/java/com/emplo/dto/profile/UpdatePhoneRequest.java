package com.emplo.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePhoneRequest {
    @NotBlank
    private String mobileNumber;
}
