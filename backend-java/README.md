# Emplo HRMS Backend - Java Spring Boot

Complete migration of the Python FastAPI backend to Java Spring Boot 3.x.

## Technology Stack

- Java 21
- Spring Boot 3.3.0
- Spring Web, Spring Security, Spring Data JPA
- PostgreSQL (same database as the Python version)
- JWT authentication (jjwt 0.12.6)
- Lombok, Jackson, Validation annotations
- iText7 (PDF generation)
- Apache Commons CSV (data export)
- Bucket4j (rate limiting)
- BCrypt password hashing
- AES encryption for sensitive data (bank accounts)

## Prerequisites

- Java 21+ (JDK)
- Maven 3.9+
- PostgreSQL 14+ (same instance used by the Python backend)

## Configuration

Set environment variables or create an `application-local.yml`:

```yaml
# Database
DATABASE_URL: jdbc:postgresql://localhost:5432/hr_portal
DB_USERNAME: postgres
DB_PASSWORD: postgres

# JWT
JWT_SECRET_KEY: change-me-to-a-long-random-string
ACCESS_TOKEN_EXPIRE_MINUTES: 30
REFRESH_TOKEN_EXPIRE_DAYS: 7
```

Or set as environment variables:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/hr_portal
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET_KEY=change-me-to-a-long-random-string
```

## Build

```bash
cd backend-java
mvn clean package -DskipTests
```

## Run

```bash
mvn spring-boot:run
```

Or with the JAR:
```bash
java -jar target/emplo-backend-1.0.0.jar
```

The server starts on port **8000** (same as the Python backend).

## Seed Data

On first startup, the application automatically:
1. Creates roles: `employee`, `manager`, `hr_admin`
2. Creates the default HR admin account:
   - Email: `saransrini@company.com`
   - Password: `Secret123`

## API Endpoints

All endpoints are identical to the Python backend:

| Module | Prefix | Description |
|--------|--------|-------------|
| Health | `/health` | Liveness and readiness checks |
| Auth | `/auth` | Register, login, refresh, logout, me |
| Password Reset | `/password-reset` | Request and confirm password reset |
| Employees | `/employees` | CRUD, bulk import, login creation, roles |
| Profile | `/profile` | View/edit own profile, photo management |
| Attendance (Leave) | `/attendance` | Leave requests with manager→HR workflow |
| Attendance Records | `/attendance-records` | Check-in/out, time tracking |
| Leave Management | `/leave-management` | Leave types, balance allocation |
| Salary | `/salary` | Revision history, approval workflow |
| Salary Structure | `/salary-structure` | Component-level breakdown |
| Documents | `/documents` | Upload, list, verify |
| Certifications | `/certifications` | Add, list, expiring alerts |
| Performance | `/performance` | Reviews and ratings |
| Bank Accounts | `/bank-accounts` | Encrypted account management |
| Departments | `/departments` | Department and designation CRUD |
| Holidays | `/holidays` | Calendar and holiday management |
| Tickets | `/tickets` | Unified request system with comments |
| Tasks | `/tasks` | Manager-assigned task tracking |
| Messages | `/messages` | Private employee messaging |
| Notifications | `/notifications` | System notifications |
| Permissions | `/permissions` | Time-bounded edit access |
| Edit Requests | `/edit-requests` | Profile edit access workflow |
| Profile Changes | `/profile-changes` | Change request approval |
| Dashboard | `/dashboard` | Employee, manager, HR dashboards |
| Audit | `/audit` | Audit log viewing (HR only) |
| Export | `/export` | CSV data export |
| Upload | `/upload` | File upload (returns base64 data URL) |
| OCR | `/ocr` | Certificate text parsing |
| Payslip | `/payslip` | PDF payslip generation |

## Authentication

All endpoints except health checks, login, register, refresh, and password reset require a Bearer token.

```
Authorization: Bearer <access_token>
```

## Architecture

```
src/main/java/com/emplo/
├── EmploApplication.java          # Application entry point
├── config/                        # Configuration classes
│   ├── AppProperties.java         # Custom config properties
│   ├── SecurityConfig.java        # Spring Security setup
│   ├── JacksonConfig.java         # JSON serialization
│   └── DataSeeder.java            # Initial data seeding
├── security/                      # JWT authentication
│   ├── JwtService.java            # Token creation/validation
│   ├── JwtAuthenticationFilter.java
│   └── CurrentUserProvider.java   # Get current user from context
├── entity/                        # JPA entities
│   ├── enums/                     # All enum types
│   └── *.java                     # 26 entity classes
├── repository/                    # Spring Data JPA repositories
├── dto/                           # Request/Response DTOs
│   ├── auth/
│   ├── employee/
│   ├── profile/
│   ├── attendance/
│   └── ticket/
├── service/                       # Business logic
│   ├── AuditService.java
│   ├── AuthorizationService.java
│   ├── EncryptionService.java
│   ├── NotificationService.java
│   └── *.java                     # Domain services
├── controller/                    # REST controllers
│   └── *.java                     # 27 controllers
└── exception/                     # Custom exceptions + handler
```

## Migration Notes

- All REST endpoints maintain the same paths, HTTP methods, request/response formats
- JWT tokens are compatible if the same secret key is used
- Database schema is auto-generated by JPA (hibernate.ddl-auto=update)
- The same PostgreSQL database can be shared during migration
- Password hashes (BCrypt) are compatible between Python passlib and Spring Security
