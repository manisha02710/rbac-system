package com.rbac.repository;

import com.rbac.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link User} using JdbcTemplate.
 * // OOP: Encapsulation – fields are private, accessed via methods.
 */
@Repository // OOP: Abstraction – Spring hides implementation details.
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> USER_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setFullName(rs.getString("full_name"));
            user.setEmail(rs.getString("email"));
            user.setIsActive(rs.getBoolean("is_active"));
            // roleIds will be fetched lazily elsewhere
            return user;
        }
    };

    /** Find a user by username. */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> list = jdbcTemplate.query(sql, USER_MAPPER, username);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Find a user by ID. */
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> list = jdbcTemplate.query(sql, USER_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Find a user by email. */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> list = jdbcTemplate.query(sql, USER_MAPPER, email);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Find all users with a specific role name. */
    public List<User> findUsersByRoleName(String roleName) {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN user_roles ur ON u.user_id = ur.user_id " +
                     "JOIN roles r ON ur.role_id = r.role_id " +
                     "WHERE r.role_name = ? " +
                     "ORDER BY u.user_id ASC";
        return jdbcTemplate.query(sql, USER_MAPPER, roleName);
    }

    /** Find all users. */
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, USER_MAPPER);
    }

    /** Save a new user. Returns generated ID. */
    public Long save(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, is_active) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getEmail(),
                user.getIsActive());
        } catch (DuplicateKeyException e) {
            // To be wrapped later in a custom exception
            throw e;
        }
        // Retrieve generated key
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Enable / disable a user. */
    public int setActive(Long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        return jdbcTemplate.update(sql, active, userId);
    }

    /** Delete a user by ID. */
    public int deleteById(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }

    /** Update password hash. */
    public int updatePassword(Long userId, String newHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        return jdbcTemplate.update(sql, newHash, userId);
    }

    /** Update user full_name and email. */
    public int update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ? WHERE user_id = ?";
        return jdbcTemplate.update(sql, user.getFullName(), user.getEmail(), user.getId());
    }
}
