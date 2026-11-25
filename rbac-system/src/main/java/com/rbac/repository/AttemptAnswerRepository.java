package com.rbac.repository;

import com.rbac.model.AttemptAnswer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AttemptAnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttemptAnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<AttemptAnswer> MAPPER = new RowMapper<AttemptAnswer>() {
        @Override
        public AttemptAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            AttemptAnswer a = new AttemptAnswer();
            a.setAttemptId(rs.getLong("attempt_id"));
            a.setQuestionId(rs.getLong("question_id"));
            a.setSelectedOption(rs.getString("selected_option"));
            return a;
        }
    };

    public int save(Long attemptId, Long questionId, String selectedOption) {
        String sql = "INSERT INTO attempt_answers (attempt_id, question_id, selected_option) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE selected_option = VALUES(selected_option)";
        return jdbcTemplate.update(sql, attemptId, questionId, selectedOption);
    }

    public List<AttemptAnswer> findByAttemptId(Long attemptId) {
        String sql = "SELECT * FROM attempt_answers WHERE attempt_id = ?";
        return jdbcTemplate.query(sql, MAPPER, attemptId);
    }

    public Map<Long, String> findAnswerMapByAttemptId(Long attemptId) {
        List<AttemptAnswer> list = findByAttemptId(attemptId);
        Map<Long, String> map = new HashMap<>();
        for (AttemptAnswer a : list) {
            map.put(a.getQuestionId(), a.getSelectedOption());
        }
        return map;
    }
}
