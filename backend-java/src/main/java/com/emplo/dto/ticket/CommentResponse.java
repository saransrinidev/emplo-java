package com.emplo.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String userName;
    private String message;
    private boolean isInternal;
    private Instant createdAt;
}
