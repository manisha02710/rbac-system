package com.rbac.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO representing an attendance record joined with student and user details.
 */
public class AttendanceViewDto {
    public static final int ATTENDANCE_THRESHOLD = 75;

    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String department;
    private String subject;
    private Integer totalClasses;
    private Integer attendedClasses;
    private LocalDateTime createdAt;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AttendanceViewDto() {
    }

    public AttendanceViewDto(Long attendanceId, Long studentId, String studentName, String rollNo, String department, String subject, Integer totalClasses, Integer attendedClasses, LocalDateTime createdAt) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.rollNo = rollNo;
        this.department = department;
        this.subject = subject;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
        this.createdAt = createdAt;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(Integer totalClasses) {
        this.totalClasses = totalClasses;
    }

    public Integer getAttendedClasses() {
        return attendedClasses;
    }

    public void setAttendedClasses(Integer attendedClasses) {
        this.attendedClasses = attendedClasses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getPercentage() {
        if (totalClasses == null || totalClasses <= 0) {
            return 0;
        }
        return (int) Math.round((attendedClasses != null ? attendedClasses : 0) * 100.0 / totalClasses);
    }

    public String getStatus() {
        return getPercentage() >= ATTENDANCE_THRESHOLD ? "GOOD" : "LOW";
    }

    public boolean isLowAttendance() {
        return getPercentage() < ATTENDANCE_THRESHOLD;
    }

    public String getFormattedDate() {
        return createdAt != null ? createdAt.format(FORMATTER) : "N/A";
    }
}
