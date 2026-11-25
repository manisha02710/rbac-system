-- =====================================================================
-- Migration Script: Faculty Departments/Courses, Complaints, Announcements,
-- Materials, Assignments, Faculty Feedback, and Quizzes
-- Safe to apply to existing databases without losing data
-- =====================================================================

USE rbac_db;

-- 1. Departments table
CREATE TABLE IF NOT EXISTS departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 2. Courses table
CREATE TABLE IF NOT EXISTS courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    course_code VARCHAR(50) NOT NULL UNIQUE,
    department_id INT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 3. Faculty profiles table (1:1 with users)
CREATE TABLE IF NOT EXISTS faculty_profiles (
    faculty_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    department_id INT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. Faculty Courses junction table (Many-to-Many)
CREATE TABLE IF NOT EXISTS faculty_courses (
    faculty_id INT NOT NULL,
    course_id INT NOT NULL,
    PRIMARY KEY (faculty_id, course_id),
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Student Complaints table
CREATE TABLE IF NOT EXISTS complaints (
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

-- 6. Announcements table
CREATE TABLE IF NOT EXISTS announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    category ENUM('HOLIDAY', 'EXAM', 'EVENT', 'GENERAL') NOT NULL,
    posted_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_on DATE NULL,
    FOREIGN KEY (posted_by) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Study Materials table
CREATE TABLE IF NOT EXISTS materials (
    material_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    uploaded_by INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Assignments table
CREATE TABLE IF NOT EXISTS assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    created_by INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    due_date DATETIME NOT NULL,
    max_marks INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Submissions table
CREATE TABLE IF NOT EXISTS submissions (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT NOT NULL,
    student_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    marks_awarded INT NULL,
    feedback TEXT NULL,
    UNIQUE (assignment_id, student_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. Faculty Feedback table
CREATE TABLE IF NOT EXISTS faculty_feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    faculty_id INT NOT NULL,
    course_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, faculty_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. Quizzes table
CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    created_by INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    is_published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 12. Questions table
CREATE TABLE IF NOT EXISTS questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_option VARCHAR(5) NOT NULL,
    marks INT NOT NULL DEFAULT 1,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 13. Quiz Attempts table
CREATE TABLE IF NOT EXISTS quiz_attempts (
    attempt_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    started_at DATETIME NOT NULL,
    submitted_at DATETIME NULL,
    score INT NULL,
    UNIQUE (quiz_id, student_id),
    FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 14. Attempt Answers table
CREATE TABLE IF NOT EXISTS attempt_answers (
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_option VARCHAR(5) NULL,
    PRIMARY KEY (attempt_id, question_id),
    FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(attempt_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Seed departments
INSERT IGNORE INTO departments (department_name) VALUES
('Computer Science'),
('Mathematics'),
('Physics'),
('Electronics');

-- Seed courses
INSERT IGNORE INTO courses (course_name, course_code, department_id) VALUES
('Data Structures', 'CS101', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Java Programming', 'CS102', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Database Management Systems', 'CS103', (SELECT department_id FROM departments WHERE department_name = 'Computer Science')),
('Calculus', 'MA101', (SELECT department_id FROM departments WHERE department_name = 'Mathematics')),
('Linear Algebra', 'MA102', (SELECT department_id FROM departments WHERE department_name = 'Mathematics')),
('Physics I', 'PH101', (SELECT department_id FROM departments WHERE department_name = 'Physics')),
('Digital Electronics', 'EC101', (SELECT department_id FROM departments WHERE department_name = 'Electronics'));

-- Seed all permissions
INSERT IGNORE INTO permissions (permission_name, description) VALUES
('COMPLAINT_CREATE', 'Submit student feedback and complaints'),
('COMPLAINT_VIEW_OWN', 'View own submitted complaints'),
('COMPLAINT_MANAGE', 'Manage, review, and respond to student complaints'),
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
('QUIZ_ATTEMPT', 'Attempt course quizzes');

-- Map permissions to ADMIN (all permissions)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN';

-- Map permissions to FACULTY
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r JOIN permissions p ON p.permission_name IN (
    'STUDENT_READ', 'STUDENT_CREATE', 'MARKS_READ', 'MARKS_ADD',
    'ATTENDANCE_VIEW', 'ATTENDANCE_MANAGE', 'OWN_PROFILE_VIEW', 'OWN_MARKS_VIEW', 'OWN_ATTENDANCE_VIEW',
    'ANNOUNCEMENT_VIEW',
    'MATERIAL_MANAGE', 'MATERIAL_VIEW',
    'ASSIGNMENT_MANAGE', 'ASSIGNMENT_VIEW',
    'FEEDBACK_VIEW_OWN',
    'QUIZ_MANAGE'
)
WHERE r.role_name = 'FACULTY';

-- Map permissions to STUDENT
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r JOIN permissions p ON p.permission_name IN (
    'OWN_PROFILE_VIEW', 'OWN_MARKS_VIEW', 'OWN_ATTENDANCE_VIEW',
    'COMPLAINT_CREATE', 'COMPLAINT_VIEW_OWN',
    'ANNOUNCEMENT_VIEW',
    'MATERIAL_VIEW',
    'ASSIGNMENT_SUBMIT', 'ASSIGNMENT_VIEW',
    'FEEDBACK_SUBMIT',
    'QUIZ_ATTEMPT'
)
WHERE r.role_name = 'STUDENT';

-- Ensure faculty profile for demo faculty user (user_id 6 / username 'faculty')
INSERT IGNORE INTO faculty_profiles (user_id, department_id)
SELECT u.user_id, 1 FROM users u WHERE u.username = 'faculty';

-- Assign courses to demo faculty user
INSERT IGNORE INTO faculty_courses (faculty_id, course_id)
SELECT fp.faculty_id, 1 FROM faculty_profiles fp JOIN users u ON fp.user_id = u.user_id WHERE u.username = 'faculty';

INSERT IGNORE INTO faculty_courses (faculty_id, course_id)
SELECT fp.faculty_id, 2 FROM faculty_profiles fp JOIN users u ON fp.user_id = u.user_id WHERE u.username = 'faculty';

-- Remove ADMIN mapping for QUIZ_MANAGE and QUIZ_ATTEMPT (Quizzes are faculty-managed, student-attempted only)
DELETE FROM role_permissions 
WHERE role_id = (SELECT role_id FROM roles WHERE role_name = 'ADMIN')
  AND permission_id = (SELECT permission_id FROM permissions WHERE permission_name = 'QUIZ_MANAGE');

DELETE FROM role_permissions 
WHERE role_id = (SELECT role_id FROM roles WHERE role_name = 'ADMIN')
  AND permission_id = (SELECT permission_id FROM permissions WHERE permission_name = 'QUIZ_ATTEMPT');

-- 15. Time Extension Feature
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS extra_minutes INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS quiz_extension_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    reason TEXT NOT NULL,
    requested_minutes INT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,
    decided_by INT NULL,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (decided_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Add QUIZ_EXTENSION_REQUEST permission to STUDENT role
INSERT IGNORE INTO permissions (permission_name, description) VALUES
('QUIZ_EXTENSION_REQUEST', 'Request time extension for quiz attempts');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r JOIN permissions p ON p.permission_name = 'QUIZ_EXTENSION_REQUEST'
WHERE r.role_name = 'STUDENT';

-- 16. Quiz Extension Seen Flag for Student Notifications
ALTER TABLE quiz_extension_requests ADD COLUMN IF NOT EXISTS seen BOOLEAN DEFAULT FALSE;

