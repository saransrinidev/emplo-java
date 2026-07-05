package com.emplo.controller;

import com.emplo.service.PasswordResetService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestReset(@RequestBody ResetRequestDto request) {
        return ResponseEntity.ok(passwordResetService.requestReset(request.getEmail()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmReset(@RequestBody ResetConfirmDto request) {
        return ResponseEntity.ok(passwordResetService.confirmReset(request.getToken(), request.getNewPassword()));
    }

    @Data
    public static class ResetRequestDto {
        private String email;
    }

    @Data
    public static class ResetConfirmDto {
        private String token;
        private String newPassword;
    }
}
