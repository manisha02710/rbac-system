# Role-Based Access Control (RBAC) & Student Academic Management System

A robust, enterprise-grade Role-Based Access Control (RBAC) and Student Academic Management System implementing strict Object-Oriented Programming (OOP) principles and relational database design.

---

## 📁 Repository Structure

```
.
├── database/            # Database schema & seed SQL scripts (MySQL 8)
├── docs/                # Project reports and documentation
│   └── RBAC_Project_Report.pdf  # Comprehensive OOPS & DBMS Project Report
├── rbac-console/        # Console Edition (Java 17 CLI + JDBC application)
├── rbac-system/         # Web Edition (Spring Boot 3 + Spring JDBC + Thymeleaf)
├── DOCUMENTATION.md     # Detailed OOP & DBMS Architecture Documentation
└── README.md            # Repository overview & setup instructions
```

---

## 🚀 Key Features

### 1. Web Edition (`rbac-system`)
- **Technology Stack**: Spring Boot 3, Spring JDBC (`JdbcTemplate`), Thymeleaf, MySQL 8, BCrypt Password Hashing.
- **Role-Based Portals**:
  - **Admin**: Full user & role management, dynamic permission assignment, audit logging, complaints management, system-wide announcements.
  - **Faculty**: Student academic management, attendance tracking, marks entry, course material upload, assignment creation & grading, feedback review.
  - **Student**: View marks & GPA calculations, attendance status & alerts, view/submit assignments, attempt online quizzes with auto-grading, submit complaints, view announcements, faculty feedback.
- **Security**: Custom RBAC annotations (`@RequiresPermission`), audit trailing, secure session handling.

### 2. Console Edition (`rbac-console`)
- **Technology Stack**: Core Java 17+, JDBC, MySQL 8, PBKDF2 Password Hashing.
- **Terminal UI**: Fully interactive CLI console application with multi-level role-based navigation menus and zero web dependencies.

### 3. Database Layer (`database/`)
- Comprehensive schema with 19 normalized InnoDB tables, foreign key constraints, cascaded actions, and audit logs.

---

## 🛠 Getting Started

### Prerequisites
- **Java**: JDK 17 or higher
- **Maven**: 3.8+
- **MySQL**: 8.0+

### Database Setup
1. Start MySQL and run the initialization scripts:
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed.sql
   ```

### Running the Web Application (`rbac-system`)
```bash
cd rbac-system
mvn spring-boot:run
```
Access the application at `http://localhost:8080`.

### Running the Console Application (`rbac-console`)
```bash
cd rbac-console
mvn compile exec:java
# Or execute run.bat on Windows
```

---

## 📖 Architecture & OOP Principles
For an in-depth audit of OOP implementations (Encapsulation, Inheritance, Polymorphism, Abstraction) and database design patterns, refer to [DOCUMENTATION.md](DOCUMENTATION.md).
