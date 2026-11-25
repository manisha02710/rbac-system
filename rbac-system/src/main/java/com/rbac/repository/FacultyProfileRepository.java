package com.rbac.repository;

import com.rbac.model.Course;
import com.rbac.model.FacultyProfile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FacultyProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public FacultyProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Course> COURSE_MAPPER = new RowMapper<Course>() {
        @Override
        public Course mapRow(ResultSet rs, int rowNum) throws SQLException {
            Course c = new Course();
            c.setId(rs.getLong("course_id"));
            c.setCourseName(rs.getString("course_name"));
            c.setCourseCode(rs.getString("course_code"));
            c.setDepartmentId(rs.getLong("department_id"));
            c.setDepartmentName(rs.getString("department_name"));
            return c;
        }
    };

    /** Find profile by userId */
    public FacultyProfile findByUserId(Long userId) {
        String sql = "SELECT fp.faculty_id, fp.user_id, fp.department_id, d.department_name, " +
                     "u.full_name AS faculty_name, u.username, u.email, u.is_active " +
                     "FROM users u " +
                     "LEFT JOIN faculty_profiles fp ON u.user_id = fp.user_id " +
                     "LEFT JOIN departments d ON fp.department_id = d.department_id " +
                     "WHERE u.user_id = ?";
        try {
            FacultyProfile fp = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                FacultyProfile p = new FacultyProfile();
                long fid = rs.getLong("faculty_id");
                if (!rs.wasNull()) {
                    p.setId(fid);
                }
                p.setUserId(rs.getLong("user_id"));
                long did = rs.getLong("department_id");
                if (!rs.wasNull()) {
                    p.setDepartmentId(did);
                }
                p.setDepartmentName(rs.getString("department_name"));
                p.setFacultyName(rs.getString("faculty_name"));
                p.setUsername(rs.getString("username"));
                p.setEmail(rs.getString("email"));
                p.setIsActive(rs.getBoolean("is_active"));
                return p;
            }, userId);

            if (fp != null && fp.getId() != null) {
                fp.setCourses(findCoursesByFacultyId(fp.getId()));
            }
            return fp;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** List all faculty users with department and courses (even if profile not yet created) */
    public List<FacultyProfile> findAllFacultyWithDetails() {
        String sql = "SELECT u.user_id, u.username, u.full_name AS faculty_name, u.email, u.is_active, " +
                     "fp.faculty_id, fp.department_id, d.department_name " +
                     "FROM users u " +
                     "JOIN user_roles ur ON u.user_id = ur.user_id " +
                     "JOIN roles r ON ur.role_id = r.role_id " +
                     "LEFT JOIN faculty_profiles fp ON u.user_id = fp.user_id " +
                     "LEFT JOIN departments d ON fp.department_id = d.department_id " +
                     "WHERE r.role_name = 'FACULTY' " +
                     "ORDER BY u.full_name ASC";

        List<FacultyProfile> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            FacultyProfile p = new FacultyProfile();
            p.setUserId(rs.getLong("user_id"));
            p.setUsername(rs.getString("username"));
            p.setFacultyName(rs.getString("faculty_name"));
            p.setEmail(rs.getString("email"));
            p.setIsActive(rs.getBoolean("is_active"));
            long fid = rs.getLong("faculty_id");
            if (!rs.wasNull()) {
                p.setId(fid);
            }
            long did = rs.getLong("department_id");
            if (!rs.wasNull()) {
                p.setDepartmentId(did);
            }
            p.setDepartmentName(rs.getString("department_name"));
            return p;
        });

        for (FacultyProfile fp : list) {
            if (fp.getId() != null) {
                fp.setCourses(findCoursesByFacultyId(fp.getId()));
            }
        }
        return list;
    }

    /** Find courses assigned to a faculty profile */
    public List<Course> findCoursesByFacultyId(Long facultyId) {
        String sql = "SELECT c.*, d.department_name FROM courses c " +
                     "JOIN faculty_courses fc ON c.course_id = fc.course_id " +
                     "JOIN departments d ON c.department_id = d.department_id " +
                     "WHERE fc.faculty_id = ? " +
                     "ORDER BY c.course_code ASC";
        return jdbcTemplate.query(sql, COURSE_MAPPER, facultyId);
    }

    /** Save a new faculty profile */
    public Long saveProfile(Long userId, Long departmentId) {
        String sql = "INSERT INTO faculty_profiles (user_id, department_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            if (departmentId != null) {
                ps.setLong(2, departmentId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** Update department of existing faculty profile */
    public void updateDepartment(Long facultyId, Long departmentId) {
        String sql = "UPDATE faculty_profiles SET department_id = ? WHERE faculty_id = ?";
        jdbcTemplate.update(sql, departmentId, facultyId);
    }

    /** Assign course to faculty */
    public void assignCourse(Long facultyId, Long courseId) {
        String sql = "INSERT IGNORE INTO faculty_courses (faculty_id, course_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, facultyId, courseId);
    }

    /** Clear all courses for faculty */
    public void clearCourses(Long facultyId) {
        String sql = "DELETE FROM faculty_courses WHERE faculty_id = ?";
        jdbcTemplate.update(sql, facultyId);
    }

    /** Delete faculty profile */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM faculty_profiles WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
}
