package com.emplo.dto.ticket;

import com.emplo.entity.enums.TicketPriority;
import com.emplo.entity.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateTicketRequest {
    @NotNull
    private TicketType ticketType;
    @NotBlank
    private String subject;
    private String description;
    private TicketPriority priority = TicketPriority.medium;
    private Map<String, Object> metadata;
}
