package com.emplo.dto.employee;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateLoginResult {

    private int total;
    private int created;
    private List<String> errors;
}
