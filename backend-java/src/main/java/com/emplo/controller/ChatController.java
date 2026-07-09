package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        User user = currentUserProvider.getCurrentUser();
        String response = chatService.chat(user, request.getMessage());
        return ResponseEntity.ok(Map.of("response", response));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
