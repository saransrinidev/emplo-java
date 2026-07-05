package com.emplo.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportRequest {

    @NotEmpty(message = "Employees list must not be empty")
    @Valid
    private List<BulkImportItem> employees;
}
