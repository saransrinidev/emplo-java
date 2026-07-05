package com.emplo.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveManagerActionDto {
    @NotBlank
    private String action; // "forward" or "reject"
    private String remarks;
}
