package com.rbac.repository;

import com.rbac.model.Question;
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
public class QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Question> MAPPER = new RowMapper<Question>() {
        @Override
        public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
            Question q = new Question();
            q.setId(rs.getLong("question_id"));
            q.setQuizId(rs.getLong("quiz_id"));
            q.setQuestionText(rs.getString("question_text"));
            q.setOptionA(rs.getString("option_a"));
            q.setOptionB(rs.getString("option_b"));
            q.setOptionC(rs.getString("option_c"));
            q.setOptionD(rs.getString("option_d"));
            q.setCorrectOption(rs.getString("correct_option"));
            q.setMarks(rs.getInt("marks"));
            return q;
        }
    };

    public List<Question> findByQuizId(Long quizId) {
        String sql = "SELECT * FROM questions WHERE quiz_id = ? ORDER BY question_id ASC";
        return jdbcTemplate.query(sql, MAPPER, quizId);
    }

    /**
     * Critical Security Requirement:
     * Does NOT return correct answers to the browser before attempt is submitted!
     */
    public List<Question> findForStudentTakingQuiz(Long quizId) {
        String sql = "SELECT question_id, quiz_id, question_text, option_a, option_b, option_c, option_d, marks " +
                     "FROM questions WHERE quiz_id = ? ORDER BY question_id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Question q = new Question();
            q.setId(rs.getLong("question_id"));
            q.setQuizId(rs.getLong("quiz_id"));
            q.setQuestionText(rs.getString("question_text"));
            q.setOptionA(rs.getString("option_a"));
            q.setOptionB(rs.getString("option_b"));
            q.setOptionC(rs.getString("option_c"));
            q.setOptionD(rs.getString("option_d"));
            q.setCorrectOption(null); // Explicitly null
            q.setMarks(rs.getInt("marks"));
            return q;
        }, quizId);
    }

    public Question findById(Long id) {
        String sql = "SELECT * FROM questions WHERE question_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Question q) {
        String sql = "INSERT INTO questions (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, marks) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, q.getQuizId());
            ps.setString(2, q.getQuestionText());
            ps.setString(3, q.getOptionA());
            ps.setString(4, q.getOptionB());
            ps.setString(5, q.getOptionC());
            ps.setString(6, q.getOptionD());
            ps.setString(7, q.getCorrectOption());
            ps.setInt(8, q.getMarks());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM questions WHERE question_id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public int update(Question q) {
        String sql = "UPDATE questions SET question_text = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?, correct_option = ?, marks = ? WHERE question_id = ?";
        return jdbcTemplate.update(sql,
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getCorrectOption(),
                q.getMarks(),
                q.getId());
    }
}
