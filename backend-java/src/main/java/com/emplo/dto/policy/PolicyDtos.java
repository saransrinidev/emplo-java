package com.emplo.dto.policy;

import com.emplo.entity.enums.PolicyCategory;
import lombok.Data;

public class PolicyDtos {

    @Data
    public static class CreatePolicyRequest {
        private String title;
        private PolicyCategory category;
        private String content;
        private String attachmentUrl;
        private Boolean requiresAcknowledgement;
        private Boolean isPublished;
    }

    @Data
    public static class UpdatePolicyRequest {
        private String title;
        private PolicyCategory category;
        private String content;
        private String attachmentUrl;
        private Boolean requiresAcknowledgement;
        private Boolean isPublished;
    }
}
