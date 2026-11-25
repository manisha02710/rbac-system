package com.rbac.repository;

import com.rbac.model.Attendance;
import com.rbac.model.AttendanceViewDto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link Attendance} entities using JdbcTemplate.
 * // OOP: Encapsulation – private JdbcTemplate.
 */
@Repository // OOP: Abstraction
public class AttendanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Attendance> ATTENDANCE_MAPPER = new RowMapper<Attendance>() {
        @Override
        public Attendance mapRow(ResultSet rs, int rowNum) throws SQLException {
            Attendance a = new Attendance();
            a.setId(rs.getLong("attendance_id"));
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            a.setStudentId(rs.getLong("student_id"));
            a.setSubject(rs.getString("subject"));
            a.setTotalClasses(rs.getInt("total_classes"));
            a.setAttendedClasses(rs.getInt("attended_classes"));
            return a;
        }
    };

    /** Find all attendance records for a student */
    public List<Attendance> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM attendance WHERE student_id = ?";
        return jdbcTemplate.query(sql, ATTENDANCE_MAPPER, studentId);
    }

    /** Find all attendance records joined with student roll number, department, and user full name */
    public List<AttendanceViewDto> findAllWithStudentInfo(String search, boolean lowOnly) {
        String baseSql = "SELECT a.attendance_id, a.student_id, a.subject, a.total_classes, a.attended_classes, a.created_at, " +
                         "s.roll_no, s.department, u.full_name AS student_name " +
                         "FROM attendance a " +
                         "JOIN students s ON a.student_id = s.student_id " +
                         "JOIN users u ON s.user_id = u.user_id WHERE 1=1 ";
        RowMapper<AttendanceViewDto> mapper = (rs, rowNum) -> {
            AttendanceViewDto dto = new AttendanceViewDto();
            dto.setAttendanceId(rs.getLong("attendance_id"));
            dto.setStudentId(rs.getLong("student_id"));
            dto.setSubject(rs.getString("subject"));
            dto.setTotalClasses(rs.getInt("total_classes"));
            dto.setAttendedClasses(rs.getInt("attended_classes"));
            dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            dto.setRollNo(rs.getString("roll_no"));
            dto.setDepartment(rs.getString("department"));
            dto.setStudentName(rs.getString("student_name"));
            return dto;
        };
        List<Object> params = new java.util.ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            baseSql += "AND (LOWER(u.full_name) LIKE ? OR LOWER(s.roll_no) LIKE ? OR LOWER(a.subject) LIKE ?) ";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (lowOnly) {
            baseSql += "AND (a.total_classes > 0 AND (a.attended_classes * 100.0 / a.total_classes) < " + AttendanceViewDto.ATTENDANCE_THRESHOLD + ") ";
        }
        baseSql += "ORDER BY a.created_at DESC, a.attendance_id DESC";
        return jdbcTemplate.query(baseSql, mapper, params.toArray());
    }

    public List<AttendanceViewDto> findAllWithStudentInfo(String search) {
        return findAllWithStudentInfo(search, false);
    }

    /** Find an attendance record by primary key */
    public Attendance findById(Long attendanceId) {
        String sql = "SELECT * FROM attendance WHERE attendance_id = ?";
        List<Attendance> list = jdbcTemplate.query(sql, ATTENDANCE_MAPPER, attendanceId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Insert a new attendance record */
    public Long save(Attendance attendance) {
        String sql = "INSERT INTO attendance (student_id, subject, total_classes, attended_classes) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, attendance.getStudentId(), attendance.getSubject(), attendance.getTotalClasses(), attendance.getAttendedClasses());
        } catch (DuplicateKeyException e) {
            throw e;
        }
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Update an existing attendance record */
    public int update(Attendance attendance) {
        String sql = "UPDATE attendance SET subject = ?, total_classes = ?, attended_classes = ? WHERE attendance_id = ?";
        return jdbcTemplate.update(sql, attendance.getSubject(), attendance.getTotalClasses(), attendance.getAttendedClasses(), attendance.getId());
    }

    /** Delete an attendance record */
    public int delete(Long attendanceId) {
        String sql = "DELETE FROM attendance WHERE attendance_id = ?";
        return jdbcTemplate.update(sql, attendanceId);
    }
}
