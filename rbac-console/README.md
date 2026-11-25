# RBAC & Student Academic Management System - Console Edition (final)

Java 17+ | JDBC | MySQL 8 | no web server, no browser - everything runs in the terminal.

## Run on Windows
1. Start MySQL (service MySQL80) and edit `db.properties` (your MySQL user / password).
2. EITHER `mvn -q compile exec:java`
   OR put `mysql-connector-j-8.x.jar` in `lib\` and run `run.bat`.
3. The database `rbac_console_db`, all 19 tables, roles, 30 permissions and demo users are created automatically.

## Demo logins
| Role | Username | Password |
|---|---|---|
| Admin | admin | Admin@123 |
| Faculty | faculty | Faculty@123 |
| Student | student1 / student2 | Student@123 |

## Stage 1 (this build)
Login (PBKDF2 hashes, 3 attempts), role-based menus built from DB permissions, admin: users, roles,
dynamic permissions (grant/revoke), enable/disable, audit log. Every service re-checks permission in the DB.

## Stage 2 (added)
- Faculty: list students, add / edit / delete marks and attendance, view marks and attendance, low-attendance report (below 75%), view announcements.
- Student: own marks (with grade and average), own attendance (with % and GOOD/LOW status), submit complaints, track complaint status, view announcements.
- Admin: view students, marks and attendance, manage complaints (status + response), post and delete announcements (optional expiry date).
- New classes: `AcademicService`, `CommunicationService`, `ui/AcademicActions`. Same rules as Stage 1: permission re-checked in the service, every change audited.

## Stage 3 (added)
- Materials: faculty upload files (pdf/doc/ppt/xls/txt/zip/jpg/png, max 10 MB) for courses they teach; everyone can list, search and download (files are copied to a `downloads` folder). Only the uploader or an admin can delete.
- Assignments: faculty create (due date, max marks), view submissions, download files, grade with feedback, delete. Students see a status (PENDING, DUE SOON, MISSED, SUBMITTED, LATE, GRADED), submit a file, and may replace it before the deadline.
- Feedback: students rate faculty 1-5 per course (rating again updates). Admin sees averages and anonymous comments; faculty see only their own.
- Admin: list courses and assign courses to faculty (faculty can only work with their own courses). The demo faculty is linked to CS101-CS103 automatically.
- Uploaded files are stored in the `uploads` folder next to run.bat.

## OOP map
| Concept | Where |
|---|---|
| Encapsulation | private fields + getters in `model/*` |
| Inheritance | `BaseEntity` -> `BaseUser` -> `AdminUser`/`FacultyUser`/`StudentUser`/`CustomRoleUser`; `Role`, `Permission` extend `BaseEntity`; exceptions extend `RuntimeException`; `AdminMenu`... extend `RoleMenu` |
| Abstraction | abstract `BaseEntity`, `BaseUser`, `RoleMenu`; interface `AuthorizationService` |
| Polymorphism | `getRoleLabel()` / `describe()` overridden; each `RoleMenu` subclass supplies its own `items()`; `AuthorizationService` implemented by `JdbcAuthorizationService` |
| Packages | `model`, `exception`, `repository`, `service`, `ui`, `util` |
| Exceptions | `ResourceNotFound`, `DuplicateResource`, `AccessDenied`, `InvalidCredentials`, `Validation`, `DataAccess`; MySQL codes 1062/1451/1452/1045 -> friendly messages |

## DBMS map
19 InnoDB tables, PK/FK, UNIQUE, ENUM, ON DELETE CASCADE/SET NULL/RESTRICT; M:N tables (`user_roles`, `role_permissions`, `faculty_courses`);
JOIN (permission loading), GROUP BY + GROUP_CONCAT (user list), transactions (`Jdbc.transaction`: create user + role + student row),
PreparedStatement everywhere, audit trail table.
