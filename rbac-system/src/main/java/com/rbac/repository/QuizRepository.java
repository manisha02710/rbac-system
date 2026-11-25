package com.rbac.repository;

import com.rbac.model.Quiz;
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
public class QuizRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuizRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Quiz> MAPPER = new RowMapper<Quiz>() {
        @Override
        public Quiz mapRow(ResultSet rs, int rowNum) throws SQLException {
            Quiz q = new Quiz();
            q.setId(rs.getLong("quiz_id"));
            q.setCourseId(rs.getLong("course_id"));
            q.setCreatedBy(rs.getLong("created_by"));
            q.setTitle(rs.getString("title"));
            q.setDurationMinutes(rs.getInt("duration_minutes"));
            Timestamp startTs = rs.getTimestamp("start_time");
            if (startTs != null) {
                q.setStartTime(startTs.toLocalDateTime());
            }
            Timestamp endTs = rs.getTimestamp("end_time");
            if (endTs != null) {
                q.setEndTime(endTs.toLocalDateTime());
            }
            q.setIsPublished(rs.getBoolean("is_published"));
            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                q.setCreatedAt(createdTs.toLocalDateTime());
            }
            try {
                q.setCourseName(rs.getString("course_name"));
                q.setCourseCode(rs.getString("course_code"));
                q.setCreatorName(rs.getString("creator_name"));
                q.setQuestionCount(rs.getInt("question_count"));
                q.setTotalMarks(rs.getInt("total_marks"));
            } catch (SQLException ignored) {
            }
            return q;
        }
    };

    private static final String BASE_SELECT =
            "SELECT q.*, c.course_name, c.course_code, u.full_name AS creator_name, " +
            "(SELECT COUNT(*) FROM questions qs WHERE qs.quiz_id = q.quiz_id) AS question_count, " +
            "COALESCE((SELECT SUM(marks) FROM questions qs WHERE qs.quiz_id = q.quiz_id), 0) AS total_marks " +
            "FROM quizzes q " +
            "JOIN courses c ON q.course_id = c.course_id " +
            "JOIN users u ON q.created_by = u.user_id ";

    public List<Quiz> findAll() {
        String sql = BASE_SELECT + "ORDER BY q.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public List<Quiz> findByCreatedBy(Long userId) {
        String sql = BASE_SELECT + "WHERE q.created_by = ? ORDER BY q.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER, userId);
    }

    public List<Quiz> findPublished() {
        String sql = BASE_SELECT + "WHERE q.is_published = TRUE ORDER BY q.start_time ASC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public List<Quiz> findPublishedWithinWindowForStudent(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        String sql = BASE_SELECT +
                "WHERE q.is_published = TRUE " +
                "AND ( " +
                "     c.department_id IN ( " +
                "         SELECT d.department_id FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                "     OR c.course_name IN ( " +
                "         SELECT subject FROM marks WHERE student_id = ? " +
                "         UNION " +
                "         SELECT subject FROM attendance WHERE student_id = ? " +
                "     ) " +
                "     OR NOT EXISTS ( " +
                "         SELECT 1 FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                ") " +
                "AND ( (NOW() >= q.start_time AND NOW() <= q.end_time) " +
                "      OR (q.quiz_id IN (SELECT qa.quiz_id FROM quiz_attempts qa WHERE qa.student_id = ?)) ) " +
                "ORDER BY q.start_time ASC";
        return jdbcTemplate.query(sql, MAPPER, studentId, studentId, studentId, studentId, studentId);
    }

    public List<Quiz> findUnattemptedAvailableQuizzesForStudent(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        String sql = BASE_SELECT +
                "WHERE q.is_published = TRUE " +
                "AND (NOW() >= q.start_time AND NOW() <= q.end_time) " +
                "AND ( " +
                "     c.department_id IN ( " +
                "         SELECT d.department_id FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                "     OR c.course_name IN ( " +
                "         SELECT subject FROM marks WHERE student_id = ? " +
                "         UNION " +
                "         SELECT subject FROM attendance WHERE student_id = ? " +
                "     ) " +
                "     OR NOT EXISTS ( " +
                "         SELECT 1 FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                ") " +
                "AND NOT EXISTS ( " +
                "     SELECT 1 FROM quiz_attempts qa " +
                "     WHERE qa.quiz_id = q.quiz_id " +
                "       AND qa.student_id = ? " +
                "       AND qa.submitted_at IS NOT NULL " +
                ") " +
                "ORDER BY q.end_time ASC, q.start_time ASC";
        return jdbcTemplate.query(sql, MAPPER, studentId, studentId, studentId, studentId, studentId);
    }

    public int countUnattemptedAvailableQuizzesForStudent(Long studentId) {
        if (studentId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM quizzes q " +
                "JOIN courses c ON q.course_id = c.course_id " +
                "WHERE q.is_published = TRUE " +
                "AND (NOW() >= q.start_time AND NOW() <= q.end_time) " +
                "AND ( " +
                "     c.department_id IN ( " +
                "         SELECT d.department_id FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                "     OR c.course_name IN ( " +
                "         SELECT subject FROM marks WHERE student_id = ? " +
                "         UNION " +
                "         SELECT subject FROM attendance WHERE student_id = ? " +
                "     ) " +
                "     OR NOT EXISTS ( " +
                "         SELECT 1 FROM departments d " +
                "         JOIN students s ON s.department = d.department_name " +
                "         WHERE s.student_id = ? " +
                "     ) " +
                ") " +
                "AND NOT EXISTS ( " +
                "     SELECT 1 FROM quiz_attempts qa " +
                "     WHERE qa.quiz_id = q.quiz_id " +
                "       AND qa.student_id = ? " +
                "       AND qa.submitted_at IS NOT NULL " +
                ")";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, studentId, studentId, studentId, studentId);
        return count != null ? count : 0;
    }

    public Quiz findById(Long id) {
        String sql = BASE_SELECT + "WHERE q.quiz_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Quiz q) {
        String sql = "INSERT INTO quizzes (course_id, created_by, title, duration_minutes, start_time, end_time, is_published) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, q.getCourseId());
            ps.setLong(2, q.getCreatedBy());
            ps.setString(3, q.getTitle());
            ps.setInt(4, q.getDurationMinutes());
            ps.setTimestamp(5, Timestamp.valueOf(q.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(q.getEndTime()));
            ps.setBoolean(7, q.isPublished());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int update(Quiz q) {
        String sql = "UPDATE quizzes SET course_id = ?, title = ?, duration_minutes = ?, start_time = ?, end_time = ?, is_published = ? " +
                     "WHERE quiz_id = ?";
        return jdbcTemplate.update(sql,
                q.getCourseId(),
                q.getTitle(),
                q.getDurationMinutes(),
                Timestamp.valueOf(q.getStartTime()),
                Timestamp.valueOf(q.getEndTime()),
                q.isPublished(),
                q.getId());
    }

    public int setPublished(Long quizId, boolean published) {
        String sql = "UPDATE quizzes SET is_published = ? WHERE quiz_id = ?";
        return jdbcTemplate.update(sql, published, quizId);
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM quizzes WHERE quiz_id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
