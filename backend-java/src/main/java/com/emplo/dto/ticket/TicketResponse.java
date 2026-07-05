package com.emplo.dto.ticket;

import com.emplo.entity.enums.TicketPriority;
import com.emplo.entity.enums.TicketStatus;
import com.emplo.entity.enums.TicketType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private UUID employeeId;
    private String employeeName;
    private TicketType ticketType;
    private TicketPriority priority;
    private String subject;
    private String description;
    private TicketStatus status;
    private Map<String, Object> metadata;
    private UUID assignedTo;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String resolutionNotes;
    private List<CommentResponse> comments;
    private Instant createdAt;
    private Instant updatedAt;
}
