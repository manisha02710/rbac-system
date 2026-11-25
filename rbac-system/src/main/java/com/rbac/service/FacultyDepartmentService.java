package com.rbac.service;

import com.rbac.exception.DuplicateResourceException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.AuditLog;
import com.rbac.model.Course;
import com.rbac.model.Department;
import com.rbac.repository.CourseRepository;
import com.rbac.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final AuditService auditService;

    public FacultyDepartmentService(DepartmentRepository departmentRepository,
                                    CourseRepository courseRepository,
                                    AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.auditService = auditService;
    }

    public List<Department> listDepartments() {
        return departmentRepository.findAll();
    }

    public List<Course> listCourses() {
        return courseRepository.findAll();
    }

    public List<Course> listCoursesByDepartment(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId);
    }

    @Transactional
    public Department addDepartment(String departmentName, Long operatorId) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required.");
        }
        departmentName = departmentName.trim();
        if (departmentRepository.findByName(departmentName) != null) {
            throw new DuplicateResourceException("Department", "name", departmentName);
        }
        Department d = new Department();
        d.setDepartmentName(departmentName);
        Long id = departmentRepository.save(d);
        d.setId(id);
        auditService.log(operatorId, "DEPARTMENT_CREATE", "Created department: " + departmentName, AuditLog.Status.SUCCESS);
        return d;
    }

    @Transactional
    public void deleteDepartment(Long departmentId, Long operatorId) {
        Department dept = departmentRepository.findById(departmentId);
        if (dept == null) {
            throw new ResourceNotFoundException("Department", departmentId);
        }
        int facultyCount = departmentRepository.countFacultyUsing(departmentId);
        if (facultyCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + dept.getDepartmentName() +
                    "' because it is assigned to " + facultyCount + " faculty member(s). Reassign them first.");
        }
        int courseCount = departmentRepository.countCoursesUsing(departmentId);
        if (courseCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + dept.getDepartmentName() +
                    "' because it contains " + courseCount + " course(s). Remove or reassign them first.");
        }
        departmentRepository.deleteById(departmentId);
        auditService.log(operatorId, "DEPARTMENT_DELETE", "Deleted department: " + dept.getDepartmentName(), AuditLog.Status.SUCCESS);
    }

    @Transactional
    public Course addCourse(String courseName, String courseCode, Long departmentId, Long operatorId) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Course name is required.");
        }
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Department selection is required.");
        }
        courseName = courseName.trim();
        courseCode = courseCode.trim().toUpperCase();

        if (departmentRepository.findById(departmentId) == null) {
            throw new ResourceNotFoundException("Department", departmentId);
        }
        if (courseRepository.findByCode(courseCode) != null) {
            throw new DuplicateResourceException("Course", "code", courseCode);
        }

        Course c = new Course();
        c.setCourseName(courseName);
        c.setCourseCode(courseCode);
        c.setDepartmentId(departmentId);
        Long id = courseRepository.save(c);
        c.setId(id);
        auditService.log(operatorId, "COURSE_CREATE", "Created course: " + courseCode + " - " + courseName, AuditLog.Status.SUCCESS);
        return c;
    }

    @Transactional
    public void deleteCourse(Long courseId, Long operatorId) {
        Course course = courseRepository.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course", courseId);
        }
        int facultyCount = courseRepository.countFacultyUsing(courseId);
        if (facultyCount > 0) {
            throw new IllegalStateException("Cannot delete course '" + course.getCourseCode() +
                    "' because it is currently assigned to " + facultyCount + " faculty member(s). Unassign them first.");
        }
        courseRepository.deleteById(courseId);
        auditService.log(operatorId, "COURSE_DELETE", "Deleted course: " + course.getCourseCode(), AuditLog.Status.SUCCESS);
    }
}
