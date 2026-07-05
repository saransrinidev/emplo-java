package com.emplo.controller;

import com.emplo.dto.ticket.*;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.TicketStatus;
import com.emplo.entity.enums.TicketType;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(user, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> myTickets(
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "ticket_type", required = false) TicketType ticketType) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ticketService.myTickets(user, status, ticketType));
    }

    @GetMapping("/team")
    public ResponseEntity<List<TicketResponse>> teamTickets(
            @RequestParam(value = "status", required = false) TicketStatus status) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.manager, RoleName.hr_admin);
        return ResponseEntity.ok(ticketService.teamTickets(user, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ticketService.getTicket(user, id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id, @Valid @RequestBody AddCommentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.addComment(user, id, request));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listAll(
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "ticket_type", required = false) TicketType ticketType,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(ticketService.listAllTickets(status, ticketType, limit, offset));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateTicketStatusRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(ticketService.updateStatus(user, id, request));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.ok(ticketService.assignTicket(id, request));
    }
}
