package com.rbac.repository;

import com.rbac.model.Department;
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
public class DepartmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Department> MAPPER = new RowMapper<Department>() {
        @Override
        public Department mapRow(ResultSet rs, int rowNum) throws SQLException {
            Department d = new Department();
            d.setId(rs.getLong("department_id"));
            d.setDepartmentName(rs.getString("department_name"));
            return d;
        }
    };

    public List<Department> findAll() {
        return jdbcTemplate.query("SELECT * FROM departments ORDER BY department_name ASC", MAPPER);
    }

    public Department findById(Long id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM departments WHERE department_id = ?", MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Department findByName(String name) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM departments WHERE department_name = ?", MAPPER, name);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Department d) {
        String sql = "INSERT INTO departments (department_name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, d.getDepartmentName());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", id);
    }

    public int countCoursesUsing(Long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE department_id = ?", Integer.class, departmentId);
        return count != null ? count : 0;
    }

    public int countFacultyUsing(Long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM faculty_profiles WHERE department_id = ?", Integer.class, departmentId);
        return count != null ? count : 0;
    }
}
