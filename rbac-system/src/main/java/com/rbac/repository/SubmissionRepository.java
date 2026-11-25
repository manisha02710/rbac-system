package com.rbac.repository;

import com.rbac.model.Submission;
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
public class SubmissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Submission> MAPPER = new RowMapper<Submission>() {
        @Override
        public Submission mapRow(ResultSet rs, int rowNum) throws SQLException {
            Submission s = new Submission();
            s.setId(rs.getLong("submission_id"));
            s.setAssignmentId(rs.getLong("assignment_id"));
            s.setStudentId(rs.getLong("student_id"));
            s.setFileName(rs.getString("file_name"));
            s.setFilePath(rs.getString("file_path"));
            Timestamp ts = rs.getTimestamp("submitted_at");
            if (ts != null) {
                s.setSubmittedAt(ts.toLocalDateTime());
                s.setCreatedAt(ts.toLocalDateTime());
            }
            int marks = rs.getInt("marks_awarded");
            if (!rs.wasNull()) {
                s.setMarksAwarded(marks);
            }
            s.setFeedback(rs.getString("feedback"));
            try {
                s.setStudentName(rs.getString("student_name"));
                s.setRollNo(rs.getString("roll_no"));
            } catch (SQLException ignored) {
            }
            return s;
        }
    };

    private static final String BASE_SELECT =
            "SELECT s.*, u.full_name AS student_name, st.roll_no " +
            "FROM submissions s " +
            "JOIN students st ON s.student_id = st.student_id " +
            "JOIN users u ON st.user_id = u.user_id ";

    public List<Submission> findByAssignmentId(Long assignmentId) {
        String sql = BASE_SELECT + "WHERE s.assignment_id = ? ORDER BY s.submitted_at DESC";
        return jdbcTemplate.query(sql, MAPPER, assignmentId);
    }

    public Submission findByAssignmentAndStudent(Long assignmentId, Long studentId) {
        String sql = BASE_SELECT + "WHERE s.assignment_id = ? AND s.student_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, assignmentId, studentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Submission findById(Long id) {
        String sql = BASE_SELECT + "WHERE s.submission_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Submission s) {
        String sql = "INSERT INTO submissions (assignment_id, student_id, file_name, file_path, submitted_at) " +
                     "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, s.getAssignmentId());
            ps.setLong(2, s.getStudentId());
            ps.setString(3, s.getFileName());
            ps.setString(4, s.getFilePath());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int updateFile(Long submissionId, String fileName, String filePath) {
        String sql = "UPDATE submissions SET file_name = ?, file_path = ?, submitted_at = NOW() " +
                     "WHERE submission_id = ?";
        return jdbcTemplate.update(sql, fileName, filePath, submissionId);
    }

    public int grade(Long submissionId, Integer marks, String feedback) {
        String sql = "UPDATE submissions SET marks_awarded = ?, feedback = ? WHERE submission_id = ?";
        return jdbcTemplate.update(sql, marks, feedback, submissionId);
    }

    public List<Submission> findByStudentId(Long studentId) {
        String sql = BASE_SELECT + "WHERE s.student_id = ? ORDER BY s.submitted_at DESC";
        return jdbcTemplate.query(sql, MAPPER, studentId);
    }
}
