package com.rbac.repository;

import com.rbac.exception.DataAccessException;
import com.rbac.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/** Creates the database and all tables on first run, then inserts demo data (roles, permissions, users). */
public final class DatabaseInitializer {
    private DatabaseInitializer() { }

    private static final String[][] PERMISSIONS = {
        {"USER_CREATE", "Create users"}, {"USER_READ", "Read users"}, {"USER_UPDATE", "Update users"},
        {"USER_DISABLE", "Disable users"}, {"ROLE_CREATE", "Create roles"}, {"ROLE_ASSIGN", "Assign roles to users"},
        {"PERMISSION_ASSIGN", "Assign permissions to roles"}, {"AUDIT_VIEW", "View audit logs"},
        {"STUDENT_READ", "Read student information"}, {"STUDENT_CREATE", "Add new students"},
        {"MARKS_READ", "Read marks"}, {"MARKS_ADD", "Add and edit marks"},
        {"ATTENDANCE_VIEW", "View attendance"}, {"ATTENDANCE_MANAGE", "Manage attendance"},
        {"OWN_PROFILE_VIEW", "View own profile"}, {"OWN_MARKS_VIEW", "View own marks"},
        {"OWN_ATTENDANCE_VIEW", "View own attendance"},
        {"COMPLAINT_CREATE", "Submit complaints"}, {"COMPLAINT_VIEW_OWN", "View own complaints"},
        {"COMPLAINT_MANAGE", "Manage complaints"},
        {"ANNOUNCEMENT_MANAGE", "Create, edit and delete announcements"}, {"ANNOUNCEMENT_VIEW", "View announcements"},
        {"MATERIAL_MANAGE", "Manage study materials"}, {"MATERIAL_VIEW", "View study materials"},
        {"ASSIGNMENT_MANAGE", "Manage assignments and grade"}, {"ASSIGNMENT_SUBMIT", "Submit assignments"},
        {"ASSIGNMENT_VIEW", "View assignments"},
        {"FEEDBACK_SUBMIT", "Rate faculty"}, {"FEEDBACK_VIEW_ALL", "View all faculty feedback"},
        {"FEEDBACK_VIEW_OWN", "View own feedback"}
    };

    private static final String FACULTY_PERMS =
        "STUDENT_READ,STUDENT_CREATE,MARKS_READ,MARKS_ADD,ATTENDANCE_VIEW,ATTENDANCE_MANAGE,OWN_PROFILE_VIEW,"
        + "ANNOUNCEMENT_VIEW,MATERIAL_MANAGE,MATERIAL_VIEW,ASSIGNMENT_MANAGE,ASSIGNMENT_VIEW,FEEDBACK_VIEW_OWN";
    private static final String STUDENT_PERMS =
        "OWN_PROFILE_VIEW,OWN_MARKS_VIEW,OWN_ATTENDANCE_VIEW,COMPLAINT_CREATE,COMPLAINT_VIEW_OWN,ANNOUNCEMENT_VIEW,"
        + "MATERIAL_VIEW,ASSIGNMENT_SUBMIT,ASSIGNMENT_VIEW,FEEDBACK_SUBMIT";

    public static void run() {
        createDatabase();
        createTables();
        seed();
        linkDemoFacultyToCourses();
    }

    private static void createDatabase() {
        try (Connection c = Jdbc.openServer(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + Jdbc.dbName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw Jdbc.translate(e);
        }
    }

    private static void createTables() {
        String script;
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new DataAccessException("schema.sql was not found on the classpath.");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                script = r.lines().filter(l -> !l.trim().startsWith("--")).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not read schema.sql", e);
        }
        for (String stmt : script.split(";")) {
            if (!stmt.trim().isEmpty()) Jdbc.update(stmt.trim());
        }
    }

    private static void seed() {
        if (Jdbc.count("SELECT COUNT(*) FROM roles") > 0) return;      // already seeded
        Jdbc.transaction(() -> {
            Jdbc.update("INSERT INTO roles (role_name, description) VALUES ('ADMIN','Administrator with full access'),"
                    + "('FACULTY','Faculty member'),('STUDENT','Student user')");
            for (String[] p : PERMISSIONS)
                Jdbc.update("INSERT INTO permissions (permission_name, description) VALUES (?,?)", p[0], p[1]);

            Jdbc.update("INSERT INTO role_permissions (role_id, permission_id) SELECT r.role_id, p.permission_id "
                    + "FROM roles r CROSS JOIN permissions p WHERE r.role_name='ADMIN'");
            grant("FACULTY", FACULTY_PERMS);
            grant("STUDENT", STUDENT_PERMS);

            Jdbc.update("INSERT INTO departments (department_name) VALUES ('Computer Science'),('Mathematics'),"
                    + "('Physics'),('Electronics')");
            Jdbc.update("INSERT INTO courses (course_name, course_code, department_id) VALUES "
                    + "('Data Structures','CS101',(SELECT department_id FROM departments WHERE department_name='Computer Science')),"
                    + "('Java Programming','CS102',(SELECT department_id FROM departments WHERE department_name='Computer Science')),"
                    + "('Database Management Systems','CS103',(SELECT department_id FROM departments WHERE department_name='Computer Science')),"
                    + "('Calculus','MA101',(SELECT department_id FROM departments WHERE department_name='Mathematics')),"
                    + "('Physics I','PH101',(SELECT department_id FROM departments WHERE department_name='Physics'))");

            long admin = user("admin", "Admin@123", "Admin User", "admin@example.com", "ADMIN");
            long fac = user("faculty", "Faculty@123", "Faculty User", "faculty@example.com", "FACULTY");
            long s1 = user("student1", "Student@123", "Student One", "student1@example.com", "STUDENT");
            long s2 = user("student2", "Student@123", "Student Two", "student2@example.com", "STUDENT");

            Jdbc.update("INSERT INTO faculty_profiles (user_id, department_id) VALUES (?, "
                    + "(SELECT department_id FROM departments WHERE department_name='Computer Science'))", fac);
            Jdbc.update("INSERT INTO students (user_id, roll_no, department) VALUES (?,?,?)", s1, "CS2024001", "Computer Science");
            Jdbc.update("INSERT INTO students (user_id, roll_no, department) VALUES (?,?,?)", s2, "CS2024002", "Computer Science");
            return admin;
        });
    }

    /** Runs on every start but only acts once: the demo faculty teaches the three CS courses (needed for materials, assignments, feedback). */
    private static void linkDemoFacultyToCourses() {
        if (Jdbc.count("SELECT COUNT(*) FROM faculty_courses") > 0) return;
        Jdbc.update("INSERT IGNORE INTO faculty_courses (faculty_id, course_id) "
            + "SELECT fp.faculty_id, c.course_id FROM faculty_profiles fp JOIN users u ON u.user_id = fp.user_id "
            + "CROSS JOIN courses c WHERE u.username = 'faculty' AND c.course_code IN ('CS101','CS102','CS103')");
    }

    private static void grant(String role, String csv) {
        for (String perm : csv.split(","))
            Jdbc.update("INSERT INTO role_permissions (role_id, permission_id) SELECT r.role_id, p.permission_id "
                    + "FROM roles r, permissions p WHERE r.role_name=? AND p.permission_name=?", role, perm);
    }

    private static long user(String username, String password, String fullName, String email, String role) {
        long id = Jdbc.insert("INSERT INTO users (username, password_hash, full_name, email, is_active) VALUES (?,?,?,?,TRUE)",
                username, PasswordUtil.hash(password), fullName, email);
        Jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE role_name=?", id, role);
        return id;
    }
}
