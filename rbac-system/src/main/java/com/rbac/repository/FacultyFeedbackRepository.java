package com.rbac.repository;

import com.rbac.model.FacultyFeedback;
import com.rbac.model.FacultyFeedbackViewDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FacultyFeedbackRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public FacultyFeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<FacultyFeedback> MAPPER = new RowMapper<FacultyFeedback>() {
        @Override
        public FacultyFeedback mapRow(ResultSet rs, int rowNum) throws SQLException {
            FacultyFeedback f = new FacultyFeedback();
            f.setId(rs.getLong("feedback_id"));
            f.setStudentId(rs.getLong("student_id"));
            f.setFacultyId(rs.getLong("faculty_id"));
            f.setCourseId(rs.getLong("course_id"));
            f.setRating(rs.getInt("rating"));
            f.setComments(rs.getString("comments"));
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                f.setCreatedAt(ts.toLocalDateTime());
            }
            try {
                f.setFacultyName(rs.getString("faculty_name"));
                f.setCourseName(rs.getString("course_name"));
                f.setCourseCode(rs.getString("course_code"));
            } catch (SQLException ignored) {
            }
            return f;
        }
    };

    public int saveOrUpdate(Long studentId, Long facultyId, Long courseId, int rating, String comments) {
        String sql = "INSERT INTO faculty_feedback (student_id, faculty_id, course_id, rating, comments, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE rating = VALUES(rating), comments = VALUES(comments), created_at = NOW()";
        return jdbcTemplate.update(sql, studentId, facultyId, courseId, rating, comments);
    }

    public FacultyFeedback findByStudentFacultyCourse(Long studentId, Long facultyId, Long courseId) {
        String sql = "SELECT ff.*, u.full_name AS faculty_name, c.course_name, c.course_code " +
                     "FROM faculty_feedback ff " +
                     "JOIN faculty_profiles fp ON ff.faculty_id = fp.faculty_id " +
                     "JOIN users u ON fp.user_id = u.user_id " +
                     "JOIN courses c ON ff.course_id = c.course_id " +
                     "WHERE ff.student_id = ? AND ff.faculty_id = ? AND ff.course_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, MAPPER, studentId, facultyId, courseId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<FacultyFeedback> findByStudent(Long studentId) {
        String sql = "SELECT ff.*, u.full_name AS faculty_name, c.course_name, c.course_code " +
                     "FROM faculty_feedback ff " +
                     "JOIN faculty_profiles fp ON ff.faculty_id = fp.faculty_id " +
                     "JOIN users u ON fp.user_id = u.user_id " +
                     "JOIN courses c ON ff.course_id = c.course_id " +
                     "WHERE ff.student_id = ? ORDER BY ff.created_at DESC";
        return jdbcTemplate.query(sql, MAPPER, studentId);
    }

    public List<FacultyFeedbackViewDto> findAggregates(Long facultyIdFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT fp.faculty_id, u.full_name AS faculty_name, d.department_name, " +
                "c.course_id, c.course_name, c.course_code, " +
                "COALESCE(AVG(ff.rating), 0) AS avg_rating, " +
                "COUNT(ff.feedback_id) AS total_reviews " +
                "FROM faculty_profiles fp " +
                "JOIN users u ON fp.user_id = u.user_id " +
                "LEFT JOIN departments d ON fp.department_id = d.department_id " +
                "JOIN faculty_courses fc ON fp.faculty_id = fc.faculty_id " +
                "JOIN courses c ON fc.course_id = c.course_id " +
                "LEFT JOIN faculty_feedback ff ON fp.faculty_id = ff.faculty_id AND c.course_id = ff.course_id "
        );

        List<Object> params = new ArrayList<>();
        if (facultyIdFilter != null) {
            sql.append("WHERE fp.faculty_id = ? ");
            params.add(facultyIdFilter);
        }

        sql.append("GROUP BY fp.faculty_id, u.full_name, d.department_name, c.course_id, c.course_name, c.course_code ");
        sql.append("ORDER BY avg_rating DESC, u.full_name ASC");

        List<FacultyFeedbackViewDto> list = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            FacultyFeedbackViewDto dto = new FacultyFeedbackViewDto();
            dto.setFacultyId(rs.getLong("faculty_id"));
            dto.setFacultyName(rs.getString("faculty_name"));
            dto.setDepartmentName(rs.getString("department_name"));
            dto.setCourseId(rs.getLong("course_id"));
            dto.setCourseName(rs.getString("course_name"));
            dto.setCourseCode(rs.getString("course_code"));
            dto.setAverageRating(Math.round(rs.getDouble("avg_rating") * 10.0) / 10.0);
            dto.setTotalReviews(rs.getInt("total_reviews"));
            return dto;
        }, params.toArray());

        // Attach anonymous comments strictly WITHOUT student details
        for (FacultyFeedbackViewDto dto : list) {
            String commentSql = "SELECT rating, comments, created_at FROM faculty_feedback " +
                                "WHERE faculty_id = ? AND course_id = ? AND comments IS NOT NULL AND TRIM(comments) != '' " +
                                "ORDER BY created_at DESC";
            List<FacultyFeedbackViewDto.AnonymousComment> comments = jdbcTemplate.query(
                    commentSql,
                    (rs, rowNum) -> new FacultyFeedbackViewDto.AnonymousComment(
                            rs.getInt("rating"),
                            rs.getString("comments"),
                            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(DATE_FMT) : ""
                    ),
                    dto.getFacultyId(), dto.getCourseId()
            );
            dto.setComments(comments);
        }

        return list;
    }
}
