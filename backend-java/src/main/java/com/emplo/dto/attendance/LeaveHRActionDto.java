package com.emplo.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveHRActionDto {
    @NotBlank
    private String action; // "approve" or "reject"
    private String remarks;
}
