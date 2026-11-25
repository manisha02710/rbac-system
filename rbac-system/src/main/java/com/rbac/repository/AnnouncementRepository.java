package com.rbac.repository;

import com.rbac.model.Announcement;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class AnnouncementRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnnouncementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Announcement> MAPPER = new RowMapper<Announcement>() {
        @Override
        public Announcement mapRow(ResultSet rs, int rowNum) throws SQLException {
            Announcement a = new Announcement();
            a.setId(rs.getLong("announcement_id"));
            a.setTitle(rs.getString("title"));
            a.setMessage(rs.getString("message"));
            a.setCategory(Announcement.Category.valueOf(rs.getString("category")));
            a.setPostedBy(rs.getLong("posted_by"));
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            Date exp = rs.getDate("expires_on");
            if (exp != null) {
                a.setExpiresOn(exp.toLocalDate());
            }
            try {
                a.setPostedByName(rs.getString("posted_by_name"));
            } catch (SQLException ignored) {
            }
            return a;
        }
    };

    public Long save(Announcement a) {
        String sql = "INSERT INTO announcements (title, message, category, posted_by, expires_on) " +
                     "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getMessage());
            ps.setString(3, a.getCategory().name());
            ps.setLong(4, a.getPostedBy());
            if (a.getExpiresOn() != null) {
                ps.setDate(5, Date.valueOf(a.getExpiresOn()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(Announcement a) {
        String sql = "UPDATE announcements SET title = ?, message = ?, category = ?, expires_on = ? " +
                     "WHERE announcement_id = ?";
        Date expDate = a.getExpiresOn() != null ? Date.valueOf(a.getExpiresOn()) : null;
        jdbcTemplate.update(sql, a.getTitle(), a.getMessage(), a.getCategory().name(), expDate, a.getId());
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM announcements WHERE announcement_id = ?", id);
    }

    public Announcement findById(Long id) {
        try {
            String sql = "SELECT a.*, u.full_name AS posted_by_name FROM announcements a " +
                         "JOIN users u ON a.posted_by = u.user_id " +
                         "WHERE a.announcement_id = ?";
            return jdbcTemplate.queryForObject(sql, MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Announcement> findAll() {
        String sql = "SELECT a.*, u.full_name AS posted_by_name FROM announcements a " +
                     "JOIN users u ON a.posted_by = u.user_id " +
                     "ORDER BY a.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public List<Announcement> findActiveNonExpired() {
        String sql = "SELECT a.*, u.full_name AS posted_by_name FROM announcements a " +
                     "JOIN users u ON a.posted_by = u.user_id " +
                     "WHERE a.expires_on IS NULL OR a.expires_on >= CURRENT_DATE() " +
                     "ORDER BY a.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER);
    }
}
