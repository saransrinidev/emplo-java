package com.emplo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Emplo HRMS API")
                        .version("1.0.0")
                        .description("""
                                ## Emplo — HR Management System REST API

                                A complete HR platform covering the full employee lifecycle.

                                ### Authentication
                                Most endpoints require a **JWT Bearer token**. To use them here:
                                1. Call `POST /auth/login` with your email & password
                                2. Copy the `access_token` from the response
                                3. Click the green **Authorize** button (top right) and paste the token
                                4. All requests will now include your token automatically

                                ### Roles
                                - **employee** — self-service (profile, leave, documents, reimbursements)
                                - **manager** — team oversight + approvals
                                - **hr_admin** — full administrative control

                                ### Default Login
                                `saransrini@company.com` / `Secret123` (HR Admin)
                                """)
                        .contact(new Contact()
                                .name("Emplo Engineering")
                                .email("support@emplo.com"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8000").description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token (without the 'Bearer ' prefix)")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Login, registration, token refresh"),
                        new Tag().name("Employees").description("Employee records, bulk import, login accounts"),
                        new Tag().name("Profile").description("Self-service profile management"),
                        new Tag().name("Onboarding").description("New hire checklist workflow"),
                        new Tag().name("Attendance & Leave").description("Check-in/out, leave requests"),
                        new Tag().name("Salary").description("Revisions, structure, payslips"),
                        new Tag().name("Reimbursements").description("Expense claims with manager + HR approval"),
                        new Tag().name("Policies").description("HR policy publishing & acknowledgement"),
                        new Tag().name("Documents").description("Document upload & verification"),
                        new Tag().name("Certifications").description("Professional certifications"),
                        new Tag().name("Tickets").description("Employee request tickets"),
                        new Tag().name("Tasks").description("Task assignment & tracking"),
                        new Tag().name("Performance").description("Performance reviews"),
                        new Tag().name("Notifications").description("In-app notifications"),
                        new Tag().name("Dashboard").description("Role-based dashboard stats"),
                        new Tag().name("Admin").description("Departments, holidays, audit logs, exports")));
    }
}
