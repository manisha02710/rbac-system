package com.rbac.repository;

import com.rbac.model.QuizAttempt;
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
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class QuizAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuizAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<QuizAttempt> MAPPER = new RowMapper<QuizAttempt>() {
        @Override
        public QuizAttempt mapRow(ResultSet rs, int rowNum) throws SQLException {
            QuizAttempt qa = new QuizAttempt();
            qa.setId(rs.getLong("attempt_id"));
            qa.setQuizId(rs.getLong("quiz_id"));
            qa.setStudentId(rs.getLong("student_id"));
            Timestamp startTs = rs.getTimestamp("started_at");
            if (startTs != null) {
                qa.setStartedAt(startTs.toLocalDateTime());
                qa.setCreatedAt(startTs.toLocalDateTime());
            }
            Timestamp subTs = rs.getTimestamp("submitted_at");
            if (subTs != null) {
                qa.setSubmittedAt(subTs.toLocalDateTime());
            }
            int sc = rs.getInt("score");
            if (!rs.wasNull()) {
                qa.setScore(sc);
            }
            try {
                qa.setQuizTitle(rs.getString("quiz_title"));
                qa.setStudentName(rs.getString("student_name"));
                qa.setRollNo(rs.getString("roll_no"));
                qa.setDepartment(rs.getString("department"));
                qa.setTotalQuizMarks(rs.getInt("total_quiz_marks"));
                qa.setExtraMinutes(rs.getInt("extra_minutes"));
            } catch (SQLException ignored) {
            }
            return qa;
        }
    };

    private static final String BASE_SELECT =
            "SELECT qa.*, q.title AS quiz_title, u.full_name AS student_name, st.roll_no, st.department, " +
            "COALESCE((SELECT SUM(marks) FROM questions qs WHERE qs.quiz_id = qa.quiz_id), 0) AS total_quiz_marks " +
            "FROM quiz_attempts qa " +
            "JOIN quizzes q ON qa.quiz_id = q.quiz_id " +
            "JOIN students st ON qa.student_id = st.student_id " +
            "JOIN users u ON st.user_id = u.user_id ";

    public QuizAttempt findByQuizAndStudent(Long quizId, Long studentId) {
        String sql = BASE_SELECT + "WHERE qa.quiz_id = ? AND qa.student_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, quizId, studentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<QuizAttempt> findByQuizId(Long quizId) {
        String sql = BASE_SELECT + "WHERE qa.quiz_id = ? ORDER BY qa.score DESC, qa.submitted_at ASC";
        return jdbcTemplate.query(sql, MAPPER, quizId);
    }

    public QuizAttempt findById(Long id) {
        String sql = BASE_SELECT + "WHERE qa.attempt_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long startAttempt(Long quizId, Long studentId, LocalDateTime startedAt) {
        return startAttempt(quizId, studentId, startedAt, 0);
    }

    public Long startAttempt(Long quizId, Long studentId, LocalDateTime startedAt, int extraMinutes) {
        String sql = "INSERT INTO quiz_attempts (quiz_id, student_id, started_at, extra_minutes) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, quizId);
            ps.setLong(2, studentId);
            ps.setTimestamp(3, Timestamp.valueOf(startedAt));
            ps.setInt(4, extraMinutes);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int addExtraMinutes(Long attemptId, int minutes) {
        String sql = "UPDATE quiz_attempts SET extra_minutes = extra_minutes + ? WHERE attempt_id = ?";
        return jdbcTemplate.update(sql, minutes, attemptId);
    }

    public int submitAttempt(Long attemptId, LocalDateTime submittedAt, int score) {
        String sql = "UPDATE quiz_attempts SET submitted_at = ?, score = ? WHERE attempt_id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(submittedAt), score, attemptId);
    }

    public List<QuizAttempt> findByStudentId(Long studentId) {
        String sql = BASE_SELECT + "WHERE qa.student_id = ? ORDER BY qa.started_at DESC";
        return jdbcTemplate.query(sql, MAPPER, studentId);
    }

    public int countByQuizId(Long quizId) {
        String sql = "SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, quizId);
        return count != null ? count : 0;
    }
}
