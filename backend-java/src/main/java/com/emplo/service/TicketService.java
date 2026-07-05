package com.emplo.service;

import com.emplo.dto.ticket.*;
import com.emplo.entity.*;
import com.emplo.entity.enums.RoleName;
import com.emplo.entity.enums.TicketStatus;
import com.emplo.entity.enums.TicketType;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public TicketResponse createTicket(User user, CreateTicketRequest request) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }

        long count = ticketRepository.countAll();
        String ticketNumber = String.format("TKT-%04d", count + 1);

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .employeeId(user.getEmployeeId())
                .ticketType(request.getTicketType())
                .priority(request.getPriority())
                .subject(request.getSubject())
                .description(request.getDescription())
                .extraData(request.getMetadata())
                .status(TicketStatus.open)
                .build();
        ticket = ticketRepository.save(ticket);

        Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        String empName = emp != null ? emp.getFullName() : "An employee";
        notificationService.notifyHrAndManager(user, "New Ticket: " + ticketNumber,
                empName + " raised a " + request.getTicketType().name() + " request: " + request.getSubject());

        return toResponse(ticket, false, null);
    }

    public List<TicketResponse> myTickets(User user, TicketStatus status, TicketType ticketType) {
        if (user.getEmployeeId() == null) return List.of();
        List<Ticket> tickets = ticketRepository.findAllByEmployeeIdOrderByCreatedAtDesc(user.getEmployeeId());
        return tickets.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> ticketType == null || t.getTicketType() == ticketType)
                .map(t -> toResponse(t, false, null))
                .toList();
    }

    public List<TicketResponse> teamTickets(User user, TicketStatus status) {
        if (user.getEmployeeId() == null) return List.of();
        if (user.getRole().getName() == RoleName.hr_admin) {
            return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .filter(t -> status == null || t.getStatus() == status)
                    .map(t -> toResponse(t, false, null))
                    .toList();
        }
        List<UUID> reportIds = employeeRepository.findAllByManagerId(user.getEmployeeId())
                .stream().map(Employee::getId).toList();
        if (reportIds.isEmpty()) return List.of();
        return ticketRepository.findAllByEmployeeIdInOrderByCreatedAtDesc(reportIds).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .map(t -> toResponse(t, false, null))
                .toList();
    }

    public TicketResponse getTicket(User user, UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (user.getRole().getName() == RoleName.employee && !ticket.getEmployeeId().equals(user.getEmployeeId())) {
            throw new ForbiddenException("Not authorized to view this ticket");
        }
        if (user.getRole().getName() == RoleName.manager) {
            Employee emp = employeeRepository.findById(ticket.getEmployeeId()).orElse(null);
            if (emp != null && !user.getEmployeeId().equals(emp.getManagerId()) && !ticket.getEmployeeId().equals(user.getEmployeeId())) {
                throw new ForbiddenException("Not authorized to view this ticket");
            }
        }
        boolean filterInternal = user.getRole().getName() != RoleName.hr_admin;
        return toResponse(ticket, true, filterInternal ? Boolean.FALSE : null);
    }

    @Transactional
    public CommentResponse addComment(User user, UUID ticketId, AddCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (user.getRole().getName() == RoleName.employee && !ticket.getEmployeeId().equals(user.getEmployeeId())) {
            throw new ForbiddenException("Not authorized");
        }

        boolean isInternal = request.isInternal() && user.getRole().getName() == RoleName.hr_admin;

        TicketComment comment = TicketComment.builder()
                .ticketId(ticket.getId())
                .userId(user.getId())
                .message(request.getMessage())
                .isInternal(isInternal)
                .build();
        comment = ticketCommentRepository.save(comment);

        if (ticket.getStatus() == TicketStatus.open && user.getRole().getName() == RoleName.hr_admin) {
            ticket.setStatus(TicketStatus.in_progress);
            ticketRepository.save(ticket);
        }

        if (user.getRole().getName() == RoleName.hr_admin && !isInternal) {
            userRepository.findByEmployeeId(ticket.getEmployeeId()).ifPresent(empUser ->
                    notificationService.createNotification(empUser.getId(),
                            "Update on " + ticket.getTicketNumber(),
                            "HR responded to your ticket: " + ticket.getSubject()));
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .userId(comment.getUserId())
                .userName(getUserName(comment.getUserId()))
                .message(comment.getMessage())
                .isInternal(comment.getIsInternal())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public List<TicketResponse> listAllTickets(TicketStatus status, TicketType ticketType, int limit, int offset) {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> ticketType == null || t.getTicketType() == ticketType)
                .skip(offset)
                .limit(limit)
                .map(t -> toResponse(t, false, null))
                .toList();
    }

    @Transactional
    public TicketResponse updateStatus(User user, UUID ticketId, UpdateTicketStatusRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        ticket.setStatus(request.getStatus());
        if (request.getStatus() == TicketStatus.resolved || request.getStatus() == TicketStatus.closed) {
            ticket.setResolvedBy(user.getId());
            ticket.setResolvedAt(Instant.now());
            ticket.setResolutionNotes(request.getResolutionNotes());
        }
        ticketRepository.save(ticket);

        userRepository.findByEmployeeId(ticket.getEmployeeId()).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(),
                        "Ticket " + ticket.getTicketNumber() + " — " + request.getStatus().name().replace("_", " "),
                        request.getResolutionNotes() != null ? request.getResolutionNotes() : "Your request has been updated."));

        return toResponse(ticket, false, null);
    }

    @Transactional
    public TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        ticket.setAssignedTo(request.getAssignedTo());
        if (ticket.getStatus() == TicketStatus.open) {
            ticket.setStatus(TicketStatus.in_progress);
        }
        return toResponse(ticketRepository.save(ticket), false, null);
    }

    private TicketResponse toResponse(Ticket ticket, boolean includeComments, Boolean filterNonInternal) {
        Employee emp = employeeRepository.findById(ticket.getEmployeeId()).orElse(null);
        List<CommentResponse> comments = new ArrayList<>();
        if (includeComments) {
            List<TicketComment> allComments = ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticket.getId());
            for (TicketComment c : allComments) {
                if (filterNonInternal != null && filterNonInternal == Boolean.FALSE && c.getIsInternal()) {
                    continue;
                }
                comments.add(CommentResponse.builder()
                        .id(c.getId())
                        .ticketId(c.getTicketId())
                        .userId(c.getUserId())
                        .userName(getUserName(c.getUserId()))
                        .message(c.getMessage())
                        .isInternal(c.getIsInternal())
                        .createdAt(c.getCreatedAt())
                        .build());
            }
        }
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .employeeId(ticket.getEmployeeId())
                .employeeName(emp != null ? emp.getFullName() : null)
                .ticketType(ticket.getTicketType())
                .priority(ticket.getPriority())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .metadata(ticket.getExtraData())
                .assignedTo(ticket.getAssignedTo())
                .resolvedBy(ticket.getResolvedBy())
                .resolvedAt(ticket.getResolvedAt())
                .resolutionNotes(ticket.getResolutionNotes())
                .comments(comments)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private String getUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> {
            if (u.getEmployeeId() != null) {
                return employeeRepository.findById(u.getEmployeeId())
                        .map(Employee::getFullName).orElse(u.getEmail());
            }
            return u.getEmail();
        }).orElse(null);
    }
}
