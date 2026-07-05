package com.emplo.dto.ticket;

import com.emplo.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTicketStatusRequest {
    @NotNull
    private TicketStatus status;
    private String resolutionNotes;
}
