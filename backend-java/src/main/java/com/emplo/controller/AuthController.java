package com.emplo.controller;

import com.emplo.config.AppProperties;
import com.emplo.dto.auth.*;
import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;
    private final AppProperties appProperties;

    private static final String REFRESH_COOKIE_NAME = "emplo_refresh_token";

    @PostMapping("/register")
    public ResponseEntity<CurrentUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokens = authService.login(request);
        setRefreshCookie(response, tokens.getRefreshToken());
        // Still return refresh_token in body for backward compat (frontend can ignore it)
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest bodyRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Try cookie first, then fall back to body (backward compatible)
        String refreshToken = extractRefreshFromCookie(request);
        if (refreshToken == null && bodyRequest != null) {
            refreshToken = bodyRequest.getRefreshToken();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(refreshToken);
        TokenResponse tokens = authService.refresh(req);
        setRefreshCookie(response, tokens.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        // Clear the refresh cookie
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // expire immediately
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(authService.getCurrentUser(user));
    }

    // ─── Cookie helpers ───────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);      // Not accessible via JavaScript
        cookie.setSecure(true);        // Only sent over HTTPS (ignored on localhost in dev)
        cookie.setPath("/");           // Available for all paths
        cookie.setMaxAge(appProperties.getJwt().getRefreshTokenExpireDays() * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractRefreshFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }
}
