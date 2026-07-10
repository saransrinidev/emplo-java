package com.emplo.entity.enums;

public enum ReimbursementStatus {
    pending,            // submitted by employee, awaiting manager assurance
    manager_approved,   // manager gave assurance, awaiting HR final decision
    manager_rejected,   // manager rejected — stops here
    hr_approved,        // HR gave final approval — reimbursement will be processed
    hr_rejected,        // HR rejected after manager approval
    paid                // HR marked as paid out
}
