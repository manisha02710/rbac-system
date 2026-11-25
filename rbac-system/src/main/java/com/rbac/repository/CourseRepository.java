package com.rbac.repository;

import com.rbac.model.Course;
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
import java.util.List;

@Repository
public class CourseRepository {

    private final JdbcTemplate jdbcTemplate;

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Course> MAPPER = new RowMapper<Course>() {
        @Override
        public Course mapRow(ResultSet rs, int rowNum) throws SQLException {
            Course c = new Course();
            c.setId(rs.getLong("course_id"));
            c.setCourseName(rs.getString("course_name"));
            c.setCourseCode(rs.getString("course_code"));
            c.setDepartmentId(rs.getLong("department_id"));
            try {
                c.setDepartmentName(rs.getString("department_name"));
            } catch (SQLException ignored) {
            }
            return c;
        }
    };

    public List<Course> findAll() {
        String sql = "SELECT c.*, d.department_name FROM courses c " +
                     "JOIN departments d ON c.department_id = d.department_id " +
                     "ORDER BY c.course_code ASC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public Course findById(Long id) {
        try {
            String sql = "SELECT c.*, d.department_name FROM courses c " +
                         "JOIN departments d ON c.department_id = d.department_id " +
                         "WHERE c.course_id = ?";
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Course findByCode(String code) {
        try {
            String sql = "SELECT c.*, d.department_name FROM courses c " +
                         "JOIN departments d ON c.department_id = d.department_id " +
                         "WHERE c.course_code = ?";
            return jdbcTemplate.queryForObject(sql, MAPPER, code);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Course> findByDepartmentId(Long departmentId) {
        String sql = "SELECT c.*, d.department_name FROM courses c " +
                     "JOIN departments d ON c.department_id = d.department_id " +
                     "WHERE c.department_id = ? " +
                     "ORDER BY c.course_code ASC";
        return jdbcTemplate.query(sql, MAPPER, departmentId);
    }

    public Long save(Course c) {
        String sql = "INSERT INTO courses (course_name, course_code, department_id) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getCourseName());
            ps.setString(2, c.getCourseCode());
            ps.setLong(3, c.getDepartmentId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", id);
    }

    public int countFacultyUsing(Long courseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM faculty_courses WHERE course_id = ?", Integer.class, courseId);
        return count != null ? count : 0;
    }
}
