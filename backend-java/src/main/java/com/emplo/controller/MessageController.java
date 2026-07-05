package com.emplo.controller;

import com.emplo.entity.Message;
import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.MessageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<Message> send(@RequestBody SendMessageRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(user, request.getReceiverId(), request.getContent()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> conversations() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(messageService.getConversations(user));
    }

    @GetMapping("/with/{employeeId}")
    public ResponseEntity<List<Message>> conversation(@PathVariable UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(messageService.getConversation(user, employeeId));
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<Map<String, Object>>> contacts() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(messageService.getContacts(user));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(Map.of("count", messageService.getUnreadCount(user)));
    }

    @Data
    public static class SendMessageRequest {
        private UUID receiverId;
        private String content;
    }
}
