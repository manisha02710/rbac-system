DROP DATABASE IF EXISTS rbac_db;
CREATE DATABASE rbac_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rbac_db;

-- Users table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Roles table
CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- Permissions table
CREATE TABLE permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- Junction tables
CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Audit logs
CREATE TABLE audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    status ENUM('SUCCESS','DENIED') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Student information
CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    roll_no VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Marks table
CREATE TABLE marks (
    mark_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    marks INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Attendance table
CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    total_classes INT NOT NULL,
    attended_classes INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Departments table
CREATE TABLE departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Courses table
CREATE TABLE courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    course_code VARCHAR(50) NOT NULL UNIQUE,
    department_id INT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Faculty Profiles table
CREATE TABLE faculty_profiles (
    faculty_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    department_id INT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Faculty Courses junction table (Many-to-Many)
CREATE TABLE faculty_courses (
    faculty_id INT NOT NULL,
    course_id INT NOT NULL,
    PRIMARY KEY (faculty_id, course_id),
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Complaints table
CREATE TABLE complaints (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category ENUM('ACADEMIC', 'FACULTY', 'FACILITY', 'OTHER') NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
    admin_response TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Announcements table
CREATE TABLE announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    category ENUM('HOLIDAY', 'EXAM', 'EVENT', 'GENERAL') NOT NULL,
    posted_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_on DATE NULL,
    FOREIGN KEY (posted_by) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert roles
INSERT INTO roles (role_name, description) VALUES
('ADMIN', 'Administrator with full access'),
('FACULTY', 'Faculty member'),
('STUDENT', 'Student user');

-- Insert permissions
INSERT INTO permissions (permission_name, description) VALUES
('USER_CREATE', 'Create users'),
('USER_READ', 'Read users'),
('USER_UPDATE', 'Update users'),
('USER_DISABLE', 'Disable users'),
('ROLE_CREATE', 'Create roles'),
('ROLE_ASSIGN', 'Assign roles to users'),
('PERMISSION_ASSIGN', 'Assign permissions to roles'),
('AUDIT_VIEW', 'View audit logs'),
('STUDENT_READ', 'Read student information'),
('STUDENT_CREATE', 'Add new students'),
('MARKS_READ', 'Read marks'),
('MARKS_ADD', 'Add marks'),
('ATTENDANCE_VIEW', 'View attendance'),
('ATTENDANCE_MANAGE', 'Manage attendance'),
('OWN_PROFILE_VIEW', 'View own profile'),
('OWN_MARKS_VIEW', 'View own marks'),
('OWN_ATTENDANCE_VIEW', 'View own attendance'),
('COMPLAINT_CREATE', 'Submit student feedback and complaints'),
('COMPLAINT_VIEW_OWN', 'View own submitted complaints'),
('COMPLAINT_MANAGE', 'Manage student complaints and provide responses'),
('ANNOUNCEMENT_MANAGE', 'Create, edit, and delete announcements'),
('ANNOUNCEMENT_VIEW', 'View system announcements'),
('MATERIAL_MANAGE', 'Manage course study materials'),
('MATERIAL_VIEW', 'View and download study materials'),
('ASSIGNMENT_MANAGE', 'Manage course assignments and grade submissions'),
('ASSIGNMENT_SUBMIT', 'Submit course assignments'),
('ASSIGNMENT_VIEW', 'View course assignments and submissions'),
('FEEDBACK_SUBMIT', 'Submit feedback and ratings for faculty'),
('FEEDBACK_VIEW_ALL', 'View aggregate feedback and ratings across faculty'),
('FEEDBACK_VIEW_OWN', 'View own received feedback and ratings'),
('QUIZ_MANAGE', 'Manage course quizzes and questions'),
('QUIZ_ATTEMPT', 'Attempt course quizzes'),
('QUIZ_EXTENSION_REQUEST', 'Request time extension for quiz attempts');

-- Map role-permission relationships (sample)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r CROSS JOIN permissions p 
WHERE r.role_name = 'ADMIN' AND p.permission_name NOT IN ('QUIZ_MANAGE', 'QUIZ_ATTEMPT', 'QUIZ_EXTENSION_REQUEST');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r JOIN permissions p ON p.permission_name IN (
    'STUDENT_READ','STUDENT_CREATE','MARKS_READ','MARKS_ADD',
    'ATTENDANCE_VIEW','ATTENDANCE_MANAGE','OWN_PROFILE_VIEW','OWN_MARKS_VIEW','OWN_ATTENDANCE_VIEW',
    'ANNOUNCEMENT_VIEW',
    'MATERIAL_MANAGE','MATERIAL_VIEW',
    'ASSIGNMENT_MANAGE','ASSIGNMENT_VIEW',
    'FEEDBACK_VIEW_OWN',
    'QUIZ_MANAGE'
) WHERE r.role_name = 'FACULTY';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r JOIN permissions p ON p.permission_name IN (
    'OWN_PROFILE_VIEW','OWN_MARKS_VIEW','OWN_ATTENDANCE_VIEW',
    'COMPLAINT_CREATE','COMPLAINT_VIEW_OWN','ANNOUNCEMENT_VIEW',
    'MATERIAL_VIEW',
    'ASSIGNMENT_SUBMIT','ASSIGNMENT_VIEW',
    'FEEDBACK_SUBMIT',
    'QUIZ_ATTEMPT',
    'QUIZ_EXTENSION_REQUEST'
) WHERE r.role_name = 'STUDENT';

-- Seed departments
INSERT INTO departments (department_name) VALUES
('Computer Science'),
('Mathematics'),
('Physics'),
('Electronics');

-- Seed courses
INSERT INTO courses (course_name, course_code, department_id) VALUES
('Data Structures', 'CS101', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Java Programming', 'CS102', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Database Management Systems', 'CS103', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Calculus', 'MA101', (SELECT department_id FROM departments WHERE department_name = 'Mathematics')),
('Linear Algebra', 'MA102', (SELECT department_id FROM departments WHERE department_name = 'Mathematics')),
('Physics I', 'PH101', (SELECT department_id FROM departments WHERE department_name = 'Physics')),
('Digital Electronics', 'EC101', (SELECT department_id FROM departments WHERE department_name = 'Electronics'));

-- Insert users with placeholder password hashes
INSERT INTO users (username, password_hash, full_name, email, is_active) VALUES
('admin', '$2a$10$Ck9J8g1QeFzM2sHhV0LXeO2Yl9K4c3b5d7f6g8h0j1k2l3m4n5o6p', 'Admin User', 'admin@example.com', TRUE),
('faculty', '$2a$10$7EqJtq98hPqEX7fNZaFWV.Y6hM1QfQn4wCk/8YxA5a2VYV9aQ5K5K', 'Faculty User', 'faculty@example.com', TRUE),
('student1', '$2a$10$D1URzoVw/1vj5OeqnOy1R.eAAZUmVr6iO/se3o9iIgQwMMIEH6Xk2', 'Student One', 'student1@example.com', TRUE),
('student2', '$2a$10$huSqz7wQBIo0L3pxa/jtQuRcudQa36FwQiena.UZiVafpdEnphMOW', 'Student Two', 'student2@example.com', TRUE);

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id FROM users u JOIN roles r ON (u.username = 'admin' AND r.role_name = 'ADMIN') OR (u.username = 'faculty' AND r.role_name = 'FACULTY') OR (u.username = 'student1' AND r.role_name = 'STUDENT') OR (u.username = 'student2' AND r.role_name = 'STUDENT');

-- Insert student details
INSERT INTO students (user_id, roll_no, department) VALUES
((SELECT user_id FROM users WHERE username='student1'), 'S001', 'Computer Science'),
((SELECT user_id FROM users WHERE username='student2'), 'S002', 'Electrical Engineering');

-- Sample marks
INSERT INTO marks (student_id, subject, marks) VALUES
((SELECT student_id FROM students WHERE roll_no='S001'), 'Math', 85),
((SELECT student_id FROM students WHERE roll_no='S001'), 'Physics', 78),
((SELECT student_id FROM students WHERE roll_no='S002'), 'Math', 90),
((SELECT student_id FROM students WHERE roll_no='S002'), 'Physics', 88);

-- Sample attendance
INSERT INTO attendance (student_id, subject, total_classes, attended_classes) VALUES
((SELECT student_id FROM students WHERE roll_no='S001'), 'Math', 30, 28),
((SELECT student_id FROM students WHERE roll_no='S001'), 'Physics', 30, 27),
((SELECT student_id FROM students WHERE roll_no='S002'), 'Math', 30, 30),
((SELECT student_id FROM students WHERE roll_no='S002'), 'Physics', 30, 29);
