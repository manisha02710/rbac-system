-- seed.sql: Initial data for RBAC Management System
-- Use the same database context as schema.sql
USE rbac_db;

-- Insert Roles
INSERT INTO roles (role_name, description) VALUES
    ('ADMIN',   'System administrator with full access'),
    ('FACULTY', 'Faculty member with academic permissions'),
    ('STUDENT', 'Student with own data access');

-- Insert Permissions
INSERT INTO permissions (permission_name, description) VALUES
    ('USER_CREATE',        'Create new user accounts'),
    ('USER_READ',          'Read user information'),
    ('USER_UPDATE',        'Update user details'),
    ('USER_DISABLE',       'Disable/deactivate a user'),
    ('ROLE_CREATE',        'Create new roles'),
    ('ROLE_ASSIGN',        'Assign roles to users'),
    ('PERMISSION_ASSIGN',  'Assign permissions to roles'),
    ('AUDIT_VIEW',         'View audit logs'),
    ('STUDENT_READ',       'Read student profiles'),
    ('MARKS_READ',         'Read student marks'),
    ('MARKS_ADD',          'Add/modify student marks'),
    ('ATTENDANCE_VIEW',    'View attendance records'),
    ('ATTENDANCE_MANAGE',  'Manage attendance records'),
    ('OWN_PROFILE_VIEW',   'View own user profile'),
    ('OWN_MARKS_VIEW',     'View own marks'),
    ('OWN_ATTENDANCE_VIEW','View own attendance');

-- Role-Permission mappings (sensible defaults)
-- ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN';

-- FACULTY permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_name IN (
    'USER_READ','STUDENT_READ','MARKS_READ','MARKS_ADD','ATTENDANCE_VIEW','ATTENDANCE_MANAGE','OWN_PROFILE_VIEW','OWN_MARKS_VIEW','OWN_ATTENDANCE_VIEW')
WHERE r.role_name = 'FACULTY';

-- STUDENT permissions (only own data)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_name IN (
    'OWN_PROFILE_VIEW','OWN_MARKS_VIEW','OWN_ATTENDANCE_VIEW')
WHERE r.role_name = 'STUDENT';

-- Users (password_hash placeholder)
INSERT INTO users (username, password_hash, full_name, email, is_active) VALUES
    ('admin',   '{bcrypt_placeholder}', 'Admin User',   'admin@example.com',   TRUE),
    ('faculty', '{bcrypt_placeholder}', 'Faculty User', 'faculty@example.com', TRUE),
    ('student1','{bcrypt_placeholder}', 'Student One',  's1@example.com',      TRUE),
    ('student2','{bcrypt_placeholder}', 'Student Two',  's2@example.com',      TRUE);

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u JOIN roles r ON (
    (u.username = 'admin'   AND r.role_name = 'ADMIN') OR
    (u.username = 'faculty' AND r.role_name = 'FACULTY') OR
    (u.username LIKE 'student%' AND r.role_name = 'STUDENT')
);

-- Student specific records (one‑to‑one with user)
INSERT INTO students (user_id, roll_no, department) VALUES
    ((SELECT user_id FROM users WHERE username = 'student1'), '2023001', 'Computer Science'),
    ((SELECT user_id FROM users WHERE username = 'student2'), '2023002', 'Electrical Engineering');

-- Sample marks for each student
INSERT INTO marks (student_id, subject, marks) VALUES
    ((SELECT student_id FROM students WHERE roll_no = '2023001'), 'Mathematics', 85),
    ((SELECT student_id FROM students WHERE roll_no = '2023001'), 'Physics',     78),
    ((SELECT student_id FROM students WHERE roll_no = '2023002'), 'Mathematics', 90),
    ((SELECT student_id FROM students WHERE roll_no = '2023002'), 'Chemistry',   88);

-- Sample attendance records
INSERT INTO attendance (student_id, subject, total_classes, attended_classes) VALUES
    ((SELECT student_id FROM students WHERE roll_no = '2023001'), 'Mathematics', 40, 35),
    ((SELECT student_id FROM students WHERE roll_no = '2023001'), 'Physics',     30, 28),
    ((SELECT student_id FROM students WHERE roll_no = '2023002'), 'Mathematics', 40, 38),
    ((SELECT student_id FROM students WHERE roll_no = '2023002'), 'Chemistry',   30, 27);
