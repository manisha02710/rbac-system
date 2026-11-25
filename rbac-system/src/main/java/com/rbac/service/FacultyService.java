package com.rbac.service;

import com.rbac.exception.DuplicateResourceException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Mark;
import com.rbac.model.MarkViewDto;
import com.rbac.model.Role;
import com.rbac.model.Student;
import com.rbac.model.User;
import com.rbac.model.Attendance;
import com.rbac.model.AttendanceViewDto;
import com.rbac.repository.StudentRepository;
import com.rbac.repository.UserRepository;
import com.rbac.repository.RoleRepository;
import com.rbac.repository.MarkRepository;
import com.rbac.repository.AttendanceRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Business logic for faculty actions.
 * // OOP: Encapsulation – service hides repository details.
 */
@Service
public class FacultyService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MarkRepository markRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public FacultyService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          MarkRepository markRepository,
                          AttendanceRepository attendanceRepository,
                          AuditService auditService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.markRepository = markRepository;
        this.attendanceRepository = attendanceRepository;
        this.auditService = auditService;
    }

    /** Add a new mark for a student – permission MARKS_ADD */
    @Transactional
    public Mark addMark(Long studentId, String subject, Integer marks, Long facultyId) {
        Mark m = new Mark();
        m.setStudentId(studentId);
        m.setSubject(subject);
        m.setMarks(marks);
        Long id = markRepository.save(m);
        m.setId(id);
        auditService.log(facultyId, "MARKS_ADD", "Added mark for studentId=" + studentId, com.rbac.model.AuditLog.Status.SUCCESS);
        return m;
    }

    /** List all students – permission STUDENT_READ */
    public List<Student> listAllStudents() {
        return studentRepository.findAll();
    }

    /** List all marks across all students with search – permission MARKS_READ */
    public List<MarkViewDto> getAllMarks(String search) {
        return markRepository.findAllWithStudentInfo(search);
    }

    /** List all attendance across all students with search – permission ATTENDANCE_VIEW */
    public List<AttendanceViewDto> getAllAttendance(String search, boolean lowOnly) {
        return attendanceRepository.findAllWithStudentInfo(search, lowOnly);
    }

    public List<AttendanceViewDto> getAllAttendance(String search) {
        return getAllAttendance(search, false);
    }

    /**
     * Create a new student (atomic transaction).
     * Creates User (with STUDENT role) and linked Student profile.
     * Permission STUDENT_CREATE.
     */
    @Transactional
    public Student createStudent(String fullName, String username, String email, String password, String rollNo, String department, Long currentUserId) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (rollNo == null || rollNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number is required.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required.");
        }

        username = username.trim();
        email = email.trim();
        rollNo = rollNo.trim();
        fullName = fullName.trim();
        department = department.trim();

        if (userRepository.findByUsername(username) != null) {
            throw new DuplicateResourceException("User", "username", username);
        }
        if (userRepository.findByEmail(email) != null) {
            throw new DuplicateResourceException("User", "email", email);
        }
        if (studentRepository.findByRollNo(rollNo) != null) {
            throw new DuplicateResourceException("Student", "roll number", rollNo);
        }

        // 1. Create User
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setIsActive(true);
        Long userId = userRepository.save(user);

        // 2. Assign STUDENT role
        Role studentRole = roleRepository.findByName("STUDENT");
        if (studentRole != null) {
            roleRepository.assignRoleToUser(userId, studentRole.getId());
        }

        // 3. Create Student record
        Student student = new Student();
        student.setUserId(userId);
        student.setRollNo(rollNo);
        student.setDepartment(department);
        Long studentId = studentRepository.save(student);
        student.setId(studentId);
        student.setStudentName(fullName);

        // 4. Audit Log
        auditService.log(currentUserId, "STUDENT_CREATE", "Created student " + rollNo + " (" + username + ")", com.rbac.model.AuditLog.Status.SUCCESS);

        return student;
    }

    /** Get marks for a specific student – permission MARKS_READ */
    public List<Mark> getMarksForStudent(Long studentId) {
        return markRepository.findByStudentId(studentId);
    }

    /** Find a mark by ID */
    public Mark getMarkById(Long markId) {
        return markRepository.findById(markId);
    }

    /** Update an existing mark – permission MARKS_ADD */
    @Transactional
    public boolean updateMark(Long markId, String subject, Integer marks, Long facultyId) {
        Mark m = markRepository.findById(markId);
        if (m == null) {
            throw new ResourceNotFoundException("Mark", markId);
        }
        m.setSubject(subject);
        m.setMarks(marks);
        markRepository.update(m);
        auditService.log(facultyId, "MARKS_EDIT", "Updated mark id=" + markId + " for studentId=" + m.getStudentId(), com.rbac.model.AuditLog.Status.SUCCESS);
        return true;
    }

    /** Delete a mark – permission MARKS_ADD */
    @Transactional
    public boolean deleteMark(Long markId, Long facultyId) {
        Mark m = markRepository.findById(markId);
        if (m == null) {
            throw new ResourceNotFoundException("Mark", markId);
        }
        markRepository.delete(markId);
        auditService.log(facultyId, "MARKS_DELETE", "Deleted mark id=" + markId + " for studentId=" + m.getStudentId(), com.rbac.model.AuditLog.Status.SUCCESS);
        return true;
    }

    /** List attendance records for a student – permission ATTENDANCE_VIEW */
    public List<Attendance> getAttendanceForStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    /** Find an attendance record by ID */
    public Attendance getAttendanceById(Long attendanceId) {
        return attendanceRepository.findById(attendanceId);
    }

    /** Update an attendance record – permission ATTENDANCE_MANAGE */
    @Transactional
    public boolean updateAttendance(Long attendanceId, String subject, Integer total, Integer attended, Long facultyId) {
        Attendance a = attendanceRepository.findById(attendanceId);
        if (a == null) {
            throw new ResourceNotFoundException("Attendance", attendanceId);
        }
        a.setSubject(subject);
        a.setTotalClasses(total);
        a.setAttendedClasses(attended);
        attendanceRepository.update(a);
        auditService.log(facultyId, "ATTENDANCE_EDIT", "Updated attendance id=" + attendanceId + " for studentId=" + a.getStudentId(), com.rbac.model.AuditLog.Status.SUCCESS);
        return true;
    }

    /** Delete an attendance record – permission ATTENDANCE_MANAGE */
    @Transactional
    public boolean deleteAttendance(Long attendanceId, Long facultyId) {
        Attendance a = attendanceRepository.findById(attendanceId);
        if (a == null) {
            throw new ResourceNotFoundException("Attendance", attendanceId);
        }
        attendanceRepository.delete(attendanceId);
        auditService.log(facultyId, "ATTENDANCE_DELETE", "Deleted attendance id=" + attendanceId + " for studentId=" + a.getStudentId(), com.rbac.model.AuditLog.Status.SUCCESS);
        return true;
    }

    /** Add or update attendance for a student – permission ATTENDANCE_MANAGE */
    @Transactional
    public Attendance saveAttendance(Long studentId, String subject, Integer total, Integer attended, Long facultyId) {
        // Simple implementation: try to find existing record for subject, else insert new
        List<Attendance> existing = attendanceRepository.findByStudentId(studentId);
        Attendance att = null;
        for (Attendance a : existing) {
            if (a.getSubject().equals(subject)) {
                att = a;
                break;
            }
        }
        if (att == null) {
            att = new Attendance();
            att.setStudentId(studentId);
            att.setSubject(subject);
            att.setTotalClasses(total);
            att.setAttendedClasses(attended);
            Long id = attendanceRepository.save(att);
            att.setId(id);
        } else {
            att.setTotalClasses(total);
            att.setAttendedClasses(attended);
            attendanceRepository.update(att);
        }
        auditService.log(facultyId, "ATTENDANCE_MANAGE", "Managed attendance for studentId=" + studentId, com.rbac.model.AuditLog.Status.SUCCESS);
        return att;
    }
}
