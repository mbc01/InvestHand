# InvestHand

> A secure investment-group management platform for managing members, monthly contributions, payment status, and financial activity.

InvestHand is a backend system designed to support the management of investment groups and their members. It provides a structured REST API for recording and retrieving member contributions while enforcing authentication, authorization, validation, and business rules at the application layer.

The project demonstrates practical backend software engineering using **Java, Spring Boot, PostgreSQL, Spring Security, automated testing, and CI/CD**.

---

## 🚀 Overview

Managing group investments and monthly contributions can become difficult when records are handled manually.

InvestHand provides a centralized backend for managing contribution records while enforcing rules such as:

- One contribution per member per month
- Automatic payment-status determination
- Secure administrator-controlled writes
- Authenticated access to contribution records
- Validation of financial amounts
- Meaningful API error responses
- Automated testing
- Continuous integration

The system is designed with maintainability, security, data integrity, and reliable backend behavior in mind.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Core programming language |
| **Spring Boot 3.5.10** | Backend framework |
| **Spring Data JPA** | Data access and persistence |
| **Hibernate** | ORM |
| **PostgreSQL 16** | Relational database |
| **Spring Security** | Authentication and authorization |
| **BCrypt** | Password hashing |
| **Maven** | Build and dependency management |
| **JUnit 5** | Unit testing |
| **Mockito** | Mock-based testing |
| **MockMvc** | API testing |
| **H2** | In-memory testing database |
| **GitHub Actions** | Continuous integration |

Java 21 is required by the project configuration and CI pipeline.

---

## ✨ Key Features

### Contribution Management

- Record member contributions
- Retrieve all contributions
- Retrieve contributions for a specific member
- Prevent duplicate monthly contributions
- Validate contribution amounts
- Automatically determine payment status

### Authentication & Authorization

- Stateless HTTP Basic authentication
- BCrypt password hashing
- Role-based authorization
- Administrator-controlled write operations
- Authenticated access to contribution data
- No self-service account registration

### Data Integrity

- Financial amounts represented using `BigDecimal`
- Server-controlled payment status
- Server-controlled payment date
- DTO-based request handling
- Transactional contribution operations
- Domain-specific exception handling

### API Reliability

- Meaningful HTTP status codes
- Centralized exception handling
- Health-check endpoint
- OpenAPI documentation
- Swagger UI

### Automated Testing

The project includes automated tests covering:

- Business logic
- Authentication and authorization
- API error responses
- Data validation
- Password protection
- Application context
- Role seeding

---

## 🏗️ Architecture

The application follows a layered Spring Boot architecture:

```text
Client
   │
   ▼
Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
PostgreSQL
```

**Main Layers**

| Layer | Responsibility |
|-------|---------------|
| Controller | Handles HTTP requests and API responses. |
| Service | Contains application and business logic. |
| Repository | Provides database access through Spring Data JPA. |
| Model | Contains the application's domain entities. |
| DTO | Controls data entering and leaving the API. |
| Exception | Provides centralized handling of application errors. |
| Config | Contains security configuration, database initialization, and user authentication components. |

### 📁 Project Structure

```
src/main/java/com/investment/Group/management/
│
├── Config/
│   ├── SecurityConfig
│   ├── DatabaseSeeder
│   └── DatabaseUserDetailsService
│
├── Controller/
│   └── ContributionController
│
├── Service/
│   └── ContributionService
│
├── Repository/
│   ├── ContributionRepository
│   ├── MemberRepository
│   ├── UserRepository
│   └── RoleRepository
│
├── model/
│   ├── User
│   ├── Member
│   ├── Contribution
│   ├── Role
│   ├── Admin
│   └── Business
│
├── dto/
│   ├── ContributionRequest
│   └── ContributionResponse
│
└── Exception/
    ├── GlobalExceptionHandler
    ├── MemberNotFoundException
    └── DuplicateContributionException
```

---

## 🔐 Security

InvestHand uses stateless HTTP Basic authentication. Credentials are supplied through the `Authorization` header and the server does not maintain an authenticated session.

### Authorization Rules

| Operation | Access |
|---|---|
| Read contribution data | Authenticated users |
| Create contribution | ADMIN |
| Health endpoint | Public |
| Swagger/OpenAPI | Public |
| User registration | Not available |

Passwords are hashed using BCrypt and are never returned through API responses. CSRF protection is disabled because the API uses stateless authentication rather than browser session cookies.

---

## 🔌 REST API

### Contributions

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/contributions/{memberId}` | ADMIN | Record a contribution |
| GET | `/api/contributions` | Authenticated | List contributions |
| GET | `/api/contributions/member/{memberId}` | Authenticated | Get member contributions |
| GET | `/actuator/health` | Public | Application health check |

### Example Request

```bash
curl -X POST http://localhost:8080/api/contributions/1 \
  -u "admin:your-password" \
  -H 'Content-Type: application/json' \
  -d '{"amount": 500.00, "month": "JANUARY"}'
```

---

## 📊 Business Rules

InvestHand enforces important business rules at the backend level.

### Monthly Contribution Rule

A member can contribute at most once per month. The month value is normalized to uppercase to prevent duplicate records caused by different capitalization.

### Payment Status

Payments made on or before the 5th are recorded as:

**PAID**

Payments made after the 5th are recorded as:

**LATE**

The server determines:
- Payment status
- Payment date
- Member association

These values cannot be supplied directly by the client.

### Financial Precision

Financial values use `BigDecimal` rather than `double` to avoid floating-point precision problems when handling monetary values.

---

## ⚠️ API Error Handling

The API returns meaningful HTTP status codes instead of treating every failure as a server error.

| Status | Meaning |
|---|---|
| 400 | Invalid or malformed request |
| 401 | Missing or invalid credentials |
| 403 | Authenticated user lacks required privileges |
| 404 | Member not found |
| 409 | Duplicate contribution |

### Example:

```json
{
  "timestamp": "2026-10-08T05:00:39.070Z",
  "status": 409,
  "error": "Conflict",
  "message": "Member 2 has already made a contribution for JANUARY",
  "path": "/api/contributions"
}
```

---

## 🧪 Testing

The project uses an automated test suite with JUnit 5, Mockito, MockMvc, and H2.

Run the tests with:

```bash
./mvnw test
```

The test suite covers:

- Contribution validation
- Duplicate contribution detection
- Month normalization
- Monetary precision
- Authentication
- Authorization
- API error responses
- Password protection
- Application context
- Role initialization

Tests use an in-memory H2 database, so PostgreSQL is not required when running the test suite.

---

## 🔄 Continuous Integration

The project uses GitHub Actions for continuous integration.

The CI pipeline runs on:
- Pushes to `main`
- Pull requests targeting `main`

The workflow:
1. Sets up JDK 21
2. Builds the project
3. Runs the test suite
4. Executes Maven verification
5. Packages the application
6. Uploads the generated JAR as an artifact

This helps ensure that changes are automatically validated before being merged.

---

## ⚙️ Running Locally

### Prerequisites

Install:
- JDK 21
- PostgreSQL 16

Create a PostgreSQL database:

```sql
CREATE DATABASE Family_investment_scheme_db;
```

### Configuration

The application reads configuration from environment variables:

```bash
DB_URL=jdbc:postgresql://localhost:5432/Family_investment_scheme_db
DB_USERNAME=postgres
DB_PASSWORD=your-password

ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your-admin-password
```

No database or administrator credentials are committed to the repository.

### Build

```bash
./mvnw clean package
```

### Run

```bash
./mvnw spring-boot:run
```

The application runs on:

```
http://localhost:8080
```

---

## 📚 API Documentation

When the application is running, interactive API documentation is available through Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```
http://localhost:8080/v3/api-docs
```

---

## 🔒 Security Considerations

Security was considered throughout the application design. The project includes:

- BCrypt password hashing
- Role-based authorization
- Stateless authentication
- Protected write operations
- DTO-based request handling
- Password exclusion from JSON serialization
- Environment-based credentials
- Centralized exception handling
- No credentials committed to source control

---

## 🧠 Engineering Practices

InvestHand demonstrates several backend engineering principles:

- Layered architecture
- Separation of concerns
- RESTful API design
- DTO-based request/response handling
- Transactional operations
- Database persistence with JPA/Hibernate
- Secure password handling
- Role-based access control
- Automated testing
- Continuous integration
- Environment-based configuration
- Financial precision with `BigDecimal`
- Domain-specific exception handling

---

## 📌 Project Status

InvestHand is an actively developed software project focused on building reliable backend systems for investment-group management. Future development can extend the platform with additional management, analytics, and intelligent capabilities.

---

## 👨‍💻 Developer

**Samson Mumba**  
Computer Science student focused on:  
Software Engineering · Artificial Intelligence · Backend Systems · DevOps

GitHub: [@mbc01](https://github.com/mbc01)  
LinkedIn: [Samson Chibau Mumba](https://linkedin.com/in/samson-chibau-mumba-97b5b8396)

---

## 📄 License

Private project. All rights reserved.