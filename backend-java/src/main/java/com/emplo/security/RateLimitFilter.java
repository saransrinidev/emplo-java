package com.emplo.security;

import com.emplo.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for sensitive endpoints.
 * Tracks attempts per IP. Resets every 60 seconds.
 * Blocks after 5 login attempts or 3 password reset attempts per minute.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final int RESET_MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, BucketEntry> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> resetBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST to login and password-reset
        if ("POST".equals(method)) {
            String ip = getClientIp(request);

            if ("/auth/login".equals(path)) {
                if (!checkRate(loginBuckets, ip, LOGIN_MAX_ATTEMPTS)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"detail\":\"Too many login attempts. Please wait 1 minute.\"}");
                    return;
                }
            } else if ("/password-reset/request".equals(path)) {
                if (!checkRate(resetBuckets, ip, RESET_MAX_ATTEMPTS)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"detail\":\"Too many reset requests. Please wait 1 minute.\"}");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean checkRate(Map<String, BucketEntry> buckets, String ip, int maxAttempts) {
        long now = System.currentTimeMillis();
        BucketEntry entry = buckets.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new BucketEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return entry.count.get() <= maxAttempts;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class BucketEntry {
        final long windowStart;
        final AtomicInteger count;

        BucketEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
