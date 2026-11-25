package com.rbac.repository;

import com.rbac.model.Mark;
import com.rbac.model.MarkViewDto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link Mark} entities using JdbcTemplate.
 * // OOP: Encapsulation – private JdbcTemplate.
 */
@Repository // OOP: Abstraction
public class MarkRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Mark> MARK_MAPPER = new RowMapper<Mark>() {
        @Override
        public Mark mapRow(ResultSet rs, int rowNum) throws SQLException {
            Mark m = new Mark();
            m.setId(rs.getLong("mark_id"));
            m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            m.setStudentId(rs.getLong("student_id"));
            m.setSubject(rs.getString("subject"));
            m.setMarks(rs.getInt("marks"));
            return m;
        }
    };

    /** Find all marks for a given student */
    public List<Mark> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM marks WHERE student_id = ?";
        return jdbcTemplate.query(sql, MARK_MAPPER, studentId);
    }

    /** Find all marks joined with student roll number, department, and user full name */
    public List<MarkViewDto> findAllWithStudentInfo(String search) {
        String baseSql = "SELECT m.mark_id, m.student_id, m.subject, m.marks, m.created_at, " +
                         "s.roll_no, s.department, u.full_name AS student_name " +
                         "FROM marks m " +
                         "JOIN students s ON m.student_id = s.student_id " +
                         "JOIN users u ON s.user_id = u.user_id ";
        RowMapper<MarkViewDto> mapper = (rs, rowNum) -> {
            MarkViewDto dto = new MarkViewDto();
            dto.setMarkId(rs.getLong("mark_id"));
            dto.setStudentId(rs.getLong("student_id"));
            dto.setSubject(rs.getString("subject"));
            dto.setMarks(rs.getInt("marks"));
            dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            dto.setRollNo(rs.getString("roll_no"));
            dto.setDepartment(rs.getString("department"));
            dto.setStudentName(rs.getString("student_name"));
            return dto;
        };
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            String sql = baseSql + "WHERE LOWER(u.full_name) LIKE ? OR LOWER(s.roll_no) LIKE ? OR LOWER(m.subject) LIKE ? " +
                         "ORDER BY m.created_at DESC, m.mark_id DESC";
            return jdbcTemplate.query(sql, mapper, pattern, pattern, pattern);
        } else {
            String sql = baseSql + "ORDER BY m.created_at DESC, m.mark_id DESC";
            return jdbcTemplate.query(sql, mapper);
        }
    }

    /** Find a mark by primary key */
    public Mark findById(Long markId) {
        String sql = "SELECT * FROM marks WHERE mark_id = ?";
        List<Mark> list = jdbcTemplate.query(sql, MARK_MAPPER, markId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Insert a new mark */
    public Long save(Mark mark) {
        String sql = "INSERT INTO marks (student_id, subject, marks) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(sql, mark.getStudentId(), mark.getSubject(), mark.getMarks());
        } catch (DuplicateKeyException e) {
            throw e;
        }
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Update an existing mark (by primary key) */
    public int update(Mark mark) {
        String sql = "UPDATE marks SET subject = ?, marks = ? WHERE mark_id = ?";
        return jdbcTemplate.update(sql, mark.getSubject(), mark.getMarks(), mark.getId());
    }

    /** Delete a mark */
    public int delete(Long markId) {
        String sql = "DELETE FROM marks WHERE mark_id = ?";
        return jdbcTemplate.update(sql, markId);
    }
}
