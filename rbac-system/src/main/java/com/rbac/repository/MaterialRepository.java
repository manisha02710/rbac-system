package com.rbac.repository;

import com.rbac.model.Material;
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
import java.util.ArrayList;
import java.util.List;

@Repository
public class MaterialRepository {

    private final JdbcTemplate jdbcTemplate;

    public MaterialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Material> MAPPER = new RowMapper<Material>() {
        @Override
        public Material mapRow(ResultSet rs, int rowNum) throws SQLException {
            Material m = new Material();
            m.setId(rs.getLong("material_id"));
            m.setCourseId(rs.getLong("course_id"));
            m.setUploadedBy(rs.getLong("uploaded_by"));
            m.setTitle(rs.getString("title"));
            m.setDescription(rs.getString("description"));
            m.setFileName(rs.getString("file_name"));
            m.setFilePath(rs.getString("file_path"));
            Timestamp ts = rs.getTimestamp("uploaded_at");
            if (ts != null) {
                m.setUploadedAt(ts.toLocalDateTime());
                m.setCreatedAt(ts.toLocalDateTime());
            }
            try {
                m.setCourseName(rs.getString("course_name"));
                m.setCourseCode(rs.getString("course_code"));
                m.setUploaderName(rs.getString("uploader_name"));
            } catch (SQLException ignored) {
            }
            return m;
        }
    };

    private static final String BASE_SELECT =
            "SELECT m.*, c.course_name, c.course_code, u.full_name AS uploader_name " +
            "FROM materials m " +
            "JOIN courses c ON m.course_id = c.course_id " +
            "JOIN users u ON m.uploaded_by = u.user_id ";

    public List<Material> findAll() {
        String sql = BASE_SELECT + "ORDER BY m.uploaded_at DESC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public List<Material> findByCourseId(Long courseId) {
        String sql = BASE_SELECT + "WHERE m.course_id = ? ORDER BY m.uploaded_at DESC";
        return jdbcTemplate.query(sql, MAPPER, courseId);
    }

    public List<Material> search(String keyword, Long courseId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (courseId != null && courseId > 0) {
            sql.append("AND m.course_id = ? ");
            params.add(courseId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(m.title) LIKE ? OR LOWER(m.description) LIKE ? OR LOWER(c.course_name) LIKE ? OR LOWER(c.course_code) LIKE ?) ");
            String term = "%" + keyword.toLowerCase().trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        sql.append("ORDER BY m.uploaded_at DESC");
        return jdbcTemplate.query(sql.toString(), MAPPER, params.toArray());
    }

    public Material findById(Long id) {
        String sql = BASE_SELECT + "WHERE m.material_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long save(Material m) {
        String sql = "INSERT INTO materials (course_id, uploaded_by, title, description, file_name, file_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, m.getCourseId());
            ps.setLong(2, m.getUploadedBy());
            ps.setString(3, m.getTitle());
            ps.setString(4, m.getDescription());
            ps.setString(5, m.getFileName());
            ps.setString(6, m.getFilePath());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM materials WHERE material_id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
