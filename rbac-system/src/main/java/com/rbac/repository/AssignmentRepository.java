package com.rbac.repository;

import com.rbac.model.Assignment;
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
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Assignment> MAPPER = new RowMapper<Assignment>() {
        @Override
        public Assignment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Assignment a = new Assignment();
            a.setId(rs.getLong("assignment_id"));
            a.setCourseId(rs.getLong("course_id"));
            a.setCreatedBy(rs.getLong("created_by"));
            a.setTitle(rs.getString("title"));
            a.setDescription(rs.getString("description"));
            Timestamp dueTs = rs.getTimestamp("due_date");
            if (dueTs != null) {
                a.setDueDate(dueTs.toLocalDateTime());
            }
            a.setMaxMarks(rs.getInt("max_marks"));
            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                a.setCreatedAt(createdTs.toLocalDateTime());
            }
            try {
                a.setCourseName(rs.getString("course_name"));
                a.setCourseCode(rs.getString("course_code"));
                a.setCreatorName(rs.getString("creator_name"));
                a.setSubmissionCount(rs.getInt("submission_count"));
            } catch (SQLException ignored) {
            }
            return a;
        }
    };

    private static final String BASE_SELECT =
            "SELECT a.*, c.course_name, c.course_code, u.full_name AS creator_name, " +
            "(SELECT COUNT(*) FROM submissions s WHERE s.assignment_id = a.assignment_id) AS submission_count " +
            "FROM assignments a " +
            "JOIN courses c ON a.course_id = c.course_id " +
            "JOIN users u ON a.created_by = u.user_id ";

    public List<Assignment> findAll() {
        String sql = BASE_SELECT + "ORDER BY a.due_date ASC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public List<Assignment> findByCreatedBy(Long userId) {
        String sql = BASE_SELECT + "WHERE a.created_by = ? ORDER BY a.due_date ASC";
        return jdbcTemplate.query(sql, MAPPER, userId);
    }

    public List<Assignment> findByCourseId(Long courseId) {
        String sql = BASE_SELECT + "WHERE a.course_id = ? ORDER BY a.due_date ASC";
        return jdbcTemplate.query(sql, MAPPER, courseId);
    }

    public Assignment findById(Long id) {
        String sql = BASE_SELECT + "WHERE a.assignment_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Assignment a) {
        String sql = "INSERT INTO assignments (course_id, created_by, title, description, due_date, max_marks) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.getCourseId());
            ps.setLong(2, a.getCreatedBy());
            ps.setString(3, a.getTitle());
            ps.setString(4, a.getDescription());
            ps.setTimestamp(5, Timestamp.valueOf(a.getDueDate()));
            ps.setInt(6, a.getMaxMarks());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int update(Assignment a) {
        String sql = "UPDATE assignments SET course_id = ?, title = ?, description = ?, due_date = ?, max_marks = ? " +
                     "WHERE assignment_id = ?";
        return jdbcTemplate.update(sql,
                a.getCourseId(),
                a.getTitle(),
                a.getDescription(),
                Timestamp.valueOf(a.getDueDate()),
                a.getMaxMarks(),
                a.getId());
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM assignments WHERE assignment_id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
