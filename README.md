# Emplo — HR Management System

A full-stack enterprise HRMS built with Java Spring Boot and React.

## Project Structure

```
emplo-java/
├── backend-java/                    # Spring Boot 3.3 REST API
│   ├── src/main/java/com/emplo/
│   │   ├── config/                  # App config, security, data seeder
│   │   ├── controller/              # 30 REST controllers
│   │   ├── dto/                     # Request/Response DTOs
│   │   │   ├── auth/
│   │   │   ├── attendance/
│   │   │   ├── attendancerecord/
│   │   │   ├── employee/
│   │   │   ├── profile/
│   │   │   └── ticket/
│   │   ├── entity/                  # 31 JPA entities
│   │   │   └── enums/               # 14 enum types
│   │   ├── exception/               # Global exception handling
│   │   ├── repository/              # 31 Spring Data JPA repos
│   │   ├── security/                # JWT filter, service, user provider
│   │   └── service/                 # 31 business logic services
│   ├── src/main/resources/
│   │   └── application.yml          # Spring Boot config
│   ├── pom.xml                      # Maven dependencies
│   ├── run.bat                      # Startup script (gitignored)
│   └── README.md                    # Backend-specific docs
│
├── frontend/                        # React + TypeScript + Vite
│   ├── src/
│   │   ├── api/                     # 28 API client modules
│   │   ├── components/              # 29 shared UI components
│   │   ├── context/                 # Auth, Sidebar, Theme providers
│   │   ├── hooks/                   # Custom React hooks
│   │   ├── pages/                   # 17 page components
│   │   └── styles/                  # 5 CSS files
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts               # Dev server + API proxy config
│
└── README.md                        # This file
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Frontend | React 18, TypeScript, Vite, Lucide Icons, Framer Motion |
| Database | PostgreSQL (Supabase) |
| Auth | JWT (access + refresh tokens), BCrypt |
| Storage | Supabase Storage (documents, photos) |
| PDF | iText 7 (payslip generation) |
| Export | Apache Commons CSV |

## Quick Start

### Backend
```bash
cd backend-java
# Edit run.bat with your Supabase credentials
run.bat
```
Starts on **port 8000**.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Starts on **port 5173**, proxies API calls to port 8000.

### Default Login
- Email: `saransrini@company.com`
- Password: `Secret123`
- Role: HR Admin

## Features

### Core HR
- Employee CRUD with bulk import
- Department & designation management
- Role-based access (Employee, Manager, HR Admin)
- Org chart visualization

### Onboarding
- Checklist-driven workflow (21 pre-seeded templates)
- Auto-assigned on employee creation
- Progress tracking with categories
- Interactive app tour for first-time users

### Attendance & Leave
- Check-in/check-out time tracking
- Leave request workflow (Employee → Manager → HR)
- Leave types, balances, calendar integration
- Holiday calendar management

### Compensation
- Salary revision workflow with approval
- Auto-generated salary structure (Indian payroll norms)
- Component-level breakdown (earnings, deductions, employer contributions)
- Payslip PDF generation & download

### Documents & Certifications
- Cloud storage (Supabase Storage)
- HR verification workflow
- Certificate OCR parsing
- Expiry alerts

### Communication
- Private messaging (role-based access)
- In-app notifications
- Ticket system with comments & assignment

### Performance & Tasks
- Performance reviews with ratings
- Manager-assigned tasks with completion tracking
- Dashboard analytics

### Security & Compliance
- Encrypted bank account storage (AES)
- Audit logging (every action with IP + User-Agent)
- Rate limiting
- Password reset flow
- Time-bounded profile edit permissions
