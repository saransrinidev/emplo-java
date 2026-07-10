package com.emplo.dto.reimbursement;

import com.emplo.entity.enums.ReimbursementCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReimbursementDtos {

    @Data
    public static class SubmitClaimRequest {
        private String claimantName;
        private ReimbursementCategory category;
        private String title;
        private String description;
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String billUrl;
    }

    @Data
    public static class ActionRequest {
        private String remarks;
    }
}
