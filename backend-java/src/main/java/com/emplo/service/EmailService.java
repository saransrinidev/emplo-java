package com.emplo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends email notifications to employees.
 * Runs asynchronously so it doesn't block the main request thread.
 * If SMTP is not configured, logs the email instead of sending.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${MAIL_FROM:noreply@emplo.com}")
    private String fromAddress;

    @Value("${SMTP_USERNAME:}")
    private String smtpUsername;

    /**
     * Send a plain-text email asynchronously.
     * If SMTP credentials are not configured, the email is logged but not sent.
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (smtpUsername == null || smtpUsername.isBlank()) {
            log.info("[EMAIL NOT SENT — SMTP not configured] To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} — Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send a notification email to an employee about an event in the system.
     */
    @Async
    public void sendNotificationEmail(String toEmail, String employeeName, String title, String message) {
        if (toEmail == null || toEmail.isBlank()) return;

        String subject = "Emplo: " + title;
        String body = String.format("""
                Hi %s,

                %s

                %s

                ──────────────────────────────
                This is an automated notification from Emplo HRMS.
                Please log in to your account for more details.
                """,
                employeeName != null ? employeeName : "there",
                title,
                message);

        sendEmail(toEmail, subject, body);
    }

    /**
     * Send password reset email with the reset link/token.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Emplo: Password Reset Request";
        String body = String.format("""
                You requested a password reset for your Emplo account.

                Use this token to reset your password: %s

                This token expires in 1 hour.

                If you did not request this, please ignore this email.
                """, token);

        sendEmail(toEmail, subject, body);
    }
}
