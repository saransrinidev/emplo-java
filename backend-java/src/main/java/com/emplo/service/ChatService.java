package com.emplo.service;

import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    @Value("${app.openrouter.api-key:}")
    private String apiKey;

    @Value("${app.openrouter.model:meta-llama/llama-3.1-8b-instruct:free}")
    private String model;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final EmployeeRepository employeeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT_BASE = """
            You are Emplo Assistant, an AI helper for the Emplo HR Management System.
            
            RULES:
            1. ONLY answer questions related to the Emplo HRMS product, HR processes, and workplace topics.
            2. If a user asks about anything unrelated (politics, personal advice, coding help, general knowledge, entertainment), politely decline and redirect them to ask HR-related questions.
            3. Keep responses concise, professional, and helpful.
            4. Never reveal internal system details, database schemas, or API endpoints.
            5. Never generate harmful, offensive, or inappropriate content.
            6. If you don't know something specific to their company, suggest they contact HR.
            
            PRODUCT KNOWLEDGE:
            Emplo is an HR Management System with these features:
            - Employee profiles, departments, designations
            - Leave management (apply, approve, track balances)
            - Attendance (check-in/out, work hours tracking)
            - Salary structure & payslips (component breakdown, PDF download)
            - Documents & certifications (upload, verify, expiry alerts)
            - Performance reviews
            - Onboarding checklist for new employees
            - Tickets/requests system
            - Task management
            - Private messaging
            - Notifications
            - Bank account management
            - Holiday calendar
            - Reimbursement claims (submit bills, manager assurance, HR approval)
            - HR Policies (view, acknowledge)
            - Audit logs
            """;

    private static final String EMPLOYEE_CONTEXT = """
            
            USER ROLE: Employee
            They can: view their profile, apply for leave, check-in/out, view salary, upload documents,
            complete onboarding tasks, raise tickets, submit reimbursements, message their manager/peers/HR, 
            view their performance reviews, read and acknowledge HR policies.
            They CANNOT: manage other employees, approve leave, create salary revisions, or access HR-only features.
            """;

    private static final String MANAGER_CONTEXT = """
            
            USER ROLE: Manager
            They can: everything an employee can do PLUS view team attendance, forward/reject leave requests,
            assign tasks to direct reports, give assurance on reimbursement claims, view team tickets.
            They CANNOT: approve leave (only forward to HR), manage salary, or access HR-admin features.
            """;

    private static final String HR_CONTEXT = """
            
            USER ROLE: HR Admin
            They can: EVERYTHING — manage employees, approve/reject leave, manage salary revisions,
            verify documents, configure departments/holidays/leave types, view audit logs, send alerts,
            manage onboarding templates, export data, final approve/reject reimbursements, publish HR policies.
            """;

    private static final List<String> BLOCKED_TOPICS = List.of(
            "write code", "write a program", "programming", "javascript", "python", "java code",
            "recipe", "weather", "news", "politics", "religion", "movie", "song", "joke",
            "story", "poem", "translate", "math problem", "calculate", "homework",
            "personal advice", "relationship", "dating", "stock market", "crypto",
            "hack", "exploit", "bypass", "injection"
    );

    public String chat(User user, String message) {
        if (message == null || message.isBlank()) {
            return "Please type a question about the Emplo HR system.";
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "AI chat is not configured. Please set the OPENROUTER_API_KEY environment variable.";
        }

        // Quick content filter
        String lowerMsg = message.toLowerCase();
        for (String blocked : BLOCKED_TOPICS) {
            if (lowerMsg.contains(blocked)) {
                return "I can only help with questions about the Emplo HR system — things like leave, attendance, salary, documents, onboarding, reimbursements, or how to use features. How can I help you with HR-related topics?";
            }
        }

        // Build system prompt based on role
        String systemPrompt = SYSTEM_PROMPT_BASE;
        RoleName role = user.getRole().getName();
        if (role == RoleName.employee) {
            systemPrompt += EMPLOYEE_CONTEXT;
        } else if (role == RoleName.manager) {
            systemPrompt += MANAGER_CONTEXT;
        } else {
            systemPrompt += HR_CONTEXT;
        }

        try {
            return callOpenRouter(systemPrompt, message);
        } catch (Exception e) {
            log.error("OpenRouter chat error: {}", e.getMessage());
            return "I'm having trouble connecting to the AI service. Please try again in a moment.";
        }
    }

    private String callOpenRouter(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8000");
        headers.set("X-Title", "Emplo HRMS");

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 400,
                "temperature", 0.7
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(OPENROUTER_URL, entity, Map.class);

        if (response != null && response.containsKey("choices")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                return (String) msg.get("content");
            }
        }

        return "Sorry, I couldn't generate a response. Please try again.";
    }
}
