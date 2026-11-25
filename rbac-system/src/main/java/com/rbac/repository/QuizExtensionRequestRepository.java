package com.rbac.repository;

import com.rbac.model.QuizExtensionRequest;
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
public class QuizExtensionRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuizExtensionRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<QuizExtensionRequest> MAPPER = new RowMapper<QuizExtensionRequest>() {
        @Override
        public QuizExtensionRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            QuizExtensionRequest req = new QuizExtensionRequest();
            req.setId(rs.getLong("request_id"));
            req.setQuizId(rs.getLong("quiz_id"));
            req.setStudentId(rs.getLong("student_id"));
            req.setReason(rs.getString("reason"));
            req.setRequestedMinutes(rs.getInt("requested_minutes"));
            req.setStatus(rs.getString("status"));
            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                req.setCreatedAt(createdTs.toLocalDateTime());
            }
            Timestamp decidedTs = rs.getTimestamp("decided_at");
            if (decidedTs != null) {
                req.setDecidedAt(decidedTs.toLocalDateTime());
            }
            long decBy = rs.getLong("decided_by");
            if (!rs.wasNull()) {
                req.setDecidedBy(decBy);
            }
            try {
                boolean seen = rs.getBoolean("seen");
                if (!rs.wasNull()) {
                    req.setSeen(seen);
                }
            } catch (SQLException ignored) {
            }
            try {
                req.setQuizTitle(rs.getString("quiz_title"));
                req.setStudentName(rs.getString("student_name"));
                req.setRollNo(rs.getString("roll_no"));
                req.setDepartment(rs.getString("department"));
                req.setDecidedByName(rs.getString("decided_by_name"));
            } catch (SQLException ignored) {
            }
            return req;
        }
    };

    private static final String BASE_SELECT =
            "SELECT r.*, q.title AS quiz_title, u.full_name AS student_name, st.roll_no, st.department, " +
            "du.full_name AS decided_by_name " +
            "FROM quiz_extension_requests r " +
            "JOIN quizzes q ON r.quiz_id = q.quiz_id " +
            "JOIN students st ON r.student_id = st.student_id " +
            "JOIN users u ON st.user_id = u.user_id " +
            "LEFT JOIN users du ON r.decided_by = du.user_id ";

    public Long save(QuizExtensionRequest req) {
        String sql = "INSERT INTO quiz_extension_requests (quiz_id, student_id, reason, requested_minutes, status) " +
                     "VALUES (?, ?, ?, ?, 'PENDING')";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, req.getQuizId());
            ps.setLong(2, req.getStudentId());
            ps.setString(3, req.getReason());
            ps.setInt(4, req.getRequestedMinutes());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public QuizExtensionRequest findById(Long id) {
        String sql = BASE_SELECT + "WHERE r.request_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public QuizExtensionRequest findPendingByQuizAndStudent(Long quizId, Long studentId) {
        String sql = BASE_SELECT + "WHERE r.quiz_id = ? AND r.student_id = ? AND r.status = 'PENDING' LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, quizId, studentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public QuizExtensionRequest findLatestByQuizAndStudent(Long quizId, Long studentId) {
        String sql = BASE_SELECT + "WHERE r.quiz_id = ? AND r.student_id = ? ORDER BY r.created_at DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, quizId, studentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<QuizExtensionRequest> findByQuizId(Long quizId) {
        String sql = BASE_SELECT + "WHERE r.quiz_id = ? ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER, quizId);
    }

    public int updateStatus(Long requestId, String status, Long decidedBy, LocalDateTime decidedAt) {
        String sql = "UPDATE quiz_extension_requests SET status = ?, decided_by = ?, decided_at = ? WHERE request_id = ?";
        return jdbcTemplate.update(sql, status, decidedBy, Timestamp.valueOf(decidedAt), requestId);
    }

    public int sumApprovedMinutes(Long quizId, Long studentId) {
        String sql = "SELECT COALESCE(SUM(requested_minutes), 0) FROM quiz_extension_requests " +
                     "WHERE quiz_id = ? AND student_id = ? AND status = 'APPROVED'";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class, quizId, studentId);
        return sum != null ? sum : 0;
    }

    public List<QuizExtensionRequest> findUnseenDecidedByStudentId(Long studentId) {
        String sql = BASE_SELECT +
                     "WHERE r.student_id = ? AND r.status IN ('APPROVED', 'REJECTED') AND (r.seen IS NULL OR r.seen = FALSE) " +
                     "ORDER BY r.decided_at DESC, r.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER, studentId);
    }

    public int markSeenByIdAndStudent(Long requestId, Long studentId) {
        String sql = "UPDATE quiz_extension_requests SET seen = TRUE WHERE request_id = ? AND student_id = ?";
        return jdbcTemplate.update(sql, requestId, studentId);
    }

    public int markAllSeenForStudent(Long studentId) {
        String sql = "UPDATE quiz_extension_requests SET seen = TRUE WHERE student_id = ? AND status IN ('APPROVED', 'REJECTED') AND (seen IS NULL OR seen = FALSE)";
        return jdbcTemplate.update(sql, studentId);
    }
}
