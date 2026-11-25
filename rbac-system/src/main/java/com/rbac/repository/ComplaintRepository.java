package com.rbac.repository;

import com.rbac.model.Complaint;
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
public class ComplaintRepository {

    private final JdbcTemplate jdbcTemplate;

    public ComplaintRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Complaint> MAPPER = new RowMapper<Complaint>() {
        @Override
        public Complaint mapRow(ResultSet rs, int rowNum) throws SQLException {
            Complaint c = new Complaint();
            c.setId(rs.getLong("complaint_id"));
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            if (rs.getTimestamp("updated_at") != null) {
                c.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            c.setStudentId(rs.getLong("student_id"));
            c.setSubject(rs.getString("subject"));
            c.setDescription(rs.getString("description"));
            c.setCategory(Complaint.Category.valueOf(rs.getString("category")));
            c.setStatus(Complaint.Status.valueOf(rs.getString("status")));
            c.setAdminResponse(rs.getString("admin_response"));
            try {
                c.setStudentName(rs.getString("student_name"));
                c.setRollNo(rs.getString("roll_no"));
            } catch (SQLException ignored) {
            }
            return c;
        }
    };

    public Long save(Complaint c) {
        String sql = "INSERT INTO complaints (student_id, subject, description, category, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, c.getStudentId());
            ps.setString(2, c.getSubject());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getCategory().name());
            ps.setString(5, c.getStatus().name());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Complaint findById(Long id) {
        try {
            String sql = "SELECT c.*, u.full_name AS student_name, s.roll_no " +
                         "FROM complaints c " +
                         "JOIN students s ON c.student_id = s.student_id " +
                         "JOIN users u ON s.user_id = u.user_id " +
                         "WHERE c.complaint_id = ?";
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Complaint> findByStudentId(Long studentId) {
        String sql = "SELECT c.*, u.full_name AS student_name, s.roll_no " +
                     "FROM complaints c " +
                     "JOIN students s ON c.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE c.student_id = ? " +
                     "ORDER BY c.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER, studentId);
    }

    public List<Complaint> findAll(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            String sql = "SELECT c.*, u.full_name AS student_name, s.roll_no " +
                         "FROM complaints c " +
                         "JOIN students s ON c.student_id = s.student_id " +
                         "JOIN users u ON s.user_id = u.user_id " +
                         "WHERE c.status = ? " +
                         "ORDER BY c.created_at DESC";
            return jdbcTemplate.query(sql, MAPPER, statusFilter.toUpperCase());
        } else {
            String sql = "SELECT c.*, u.full_name AS student_name, s.roll_no " +
                         "FROM complaints c " +
                         "JOIN students s ON c.student_id = s.student_id " +
                         "JOIN users u ON s.user_id = u.user_id " +
                         "ORDER BY c.created_at DESC";
            return jdbcTemplate.query(sql, MAPPER);
        }
    }

    public void updateResponse(Long complaintId, String adminResponse, String status) {
        String sql = "UPDATE complaints SET admin_response = ?, status = ?, updated_at = NOW() WHERE complaint_id = ?";
        jdbcTemplate.update(sql, adminResponse, status, complaintId);
    }
}
