package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.service.AssignmentService;
import com.rbac.service.Services;
import com.rbac.util.Console;

import java.util.ArrayList;
import java.util.List;

/** Menu actions for materials, assignments, faculty feedback and course assignment (Stage 3). */
public class ContentActions {
    private final Services svc;
    private final BaseUser user;

    private static final String[] COURSE_COLS = {"ID", "Code", "Course", "Department"};
    private static final String[] MATERIAL_COLS = {"ID", "Course", "Title", "Description", "File", "Uploaded by", "Date"};
    private static final String[] ASSIGN_MANAGE_COLS = {"ID", "Course", "Title", "Due", "Max", "Submissions", "Ungraded"};
    private static final String[] SUBMISSION_COLS = {"Sub ID", "Roll no", "Student", "File", "Submitted", "Timing", "Marks", "Feedback"};
    private static final String[] ASSIGN_STUDENT_COLS = {"ID", "Course", "Title", "Due", "Max", "Status", "Marks", "Feedback"};
    private static final String[] AVG_COLS = {"Faculty", "Course", "Ratings", "Average", "Stars"};

    public ContentActions(Services svc, BaseUser user) {
        this.svc = svc;
        this.user = user;
    }

    // ---------------------------------------------------------------- item groups

    public List<MenuItem> materialViewItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("View study materials", "MATERIAL_VIEW", this::viewMaterials));
        l.add(new MenuItem("Search study materials", "MATERIAL_VIEW", this::searchMaterials));
        l.add(new MenuItem("Download a material", "MATERIAL_VIEW", this::downloadMaterial));
        return l;
    }

    public List<MenuItem> materialManageItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Upload a material", "MATERIAL_MANAGE", this::uploadMaterial));
        l.add(new MenuItem("Delete a material", "MATERIAL_MANAGE", this::deleteMaterial));
        return l;
    }

    public List<MenuItem> assignmentManageItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("My courses", "ASSIGNMENT_MANAGE", this::myCourses));
        l.add(new MenuItem("Create an assignment", "ASSIGNMENT_MANAGE", this::createAssignment));
        l.add(new MenuItem("List assignments", "ASSIGNMENT_MANAGE", this::listManaged));
        l.add(new MenuItem("View submissions and grade", "ASSIGNMENT_MANAGE", this::gradeSubmissions));
        l.add(new MenuItem("Delete an assignment", "ASSIGNMENT_MANAGE", this::deleteAssignment));
        return l;
    }

    public List<MenuItem> assignmentStudentItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("My assignments", "ASSIGNMENT_VIEW", this::myAssignments));
        l.add(new MenuItem("Submit an assignment", "ASSIGNMENT_SUBMIT", this::submitAssignment));
        return l;
    }

    public List<MenuItem> feedbackStudentItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Rate a faculty member", "FEEDBACK_SUBMIT", this::rateFaculty));
        l.add(new MenuItem("My feedback", "FEEDBACK_SUBMIT", this::myFeedback));
        return l;
    }

    public List<MenuItem> feedbackFacultyItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("My feedback summary", "FEEDBACK_VIEW_OWN", this::ownFeedback));
        return l;
    }

    public List<MenuItem> feedbackAdminItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Faculty feedback (averages and comments)", "FEEDBACK_VIEW_ALL", this::allFeedback));
        return l;
    }

    public List<MenuItem> courseAdminItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("List courses", "USER_READ", this::listCourses));
        l.add(new MenuItem("Assign a course to faculty", "USER_UPDATE", this::assignCourse));
        return l;
    }

    // ---------------------------------------------------------------- courses

    private void listCourses() {
        Console.title("Courses");
        Console.table(COURSE_COLS, svc.courses.listCourses());
    }

    private void myCourses() {
        Console.title("My courses");
        Console.table(COURSE_COLS, svc.courses.myCourses(user));
    }

    private void assignCourse() {
        Console.title("Assign a course to a faculty member");
        Console.table(new String[]{"Faculty ID", "Name", "Username", "Courses"}, svc.courses.listFaculty(user));
        int fid = Console.askInt("Faculty ID");
        Console.table(COURSE_COLS, svc.courses.listCourses());
        int cid = Console.askInt("Course ID");
        svc.courses.assignCourse(user, fid, cid);
        Console.ok("Course assigned.");
    }

    // ---------------------------------------------------------------- materials

    private void viewMaterials() {
        Console.title("Study materials");
        Console.table(COURSE_COLS, svc.courses.listCourses());
        int c = Console.askInt("Course ID (0 = all courses)");
        Console.table(MATERIAL_COLS, svc.materials.list(user, c));
    }

    private void searchMaterials() {
        Console.title("Search study materials");
        String k = Console.askRequired("Search word (title or description)");
        Console.table(MATERIAL_COLS, svc.materials.search(user, k));
    }

    private void downloadMaterial() {
        Console.title("Download a material");
        Console.table(MATERIAL_COLS, svc.materials.list(user, 0));
        int id = Console.askInt("Material ID");
        Console.ok("Saved to: " + svc.materials.download(user, id));
    }

    private void uploadMaterial() {
        Console.title("Upload a material");
        Console.table(COURSE_COLS, svc.courses.myCourses(user));
        int course = Console.askInt("Course ID");
        String title = Console.askRequired("Title");
        String desc = Console.ask("Description (optional)");
        String path = Console.askRequired("Full path of the file (pdf, doc, ppt, xls, txt, zip, jpg, png; max 10 MB)");
        long id = svc.materials.upload(user, course, title, desc, path);
        Console.ok("Material uploaded (ID " + id + ").");
    }

    private void deleteMaterial() {
        Console.title("Delete a material");
        Console.table(MATERIAL_COLS, svc.materials.list(user, 0));
        int id = Console.askInt("Material ID to delete");
        if (!Console.confirm("Delete material " + id + "?")) { Console.info("Cancelled."); return; }
        svc.materials.delete(user, id);
        Console.ok("Material deleted.");
    }

    // ---------------------------------------------------------------- assignments (faculty)

    private void createAssignment() {
        Console.title("Create an assignment");
        Console.table(COURSE_COLS, svc.courses.myCourses(user));
        int course = Console.askInt("Course ID");
        String title = Console.askRequired("Title");
        String desc = Console.ask("Description (optional)");
        String due = Console.askRequired("Due date and time (yyyy-mm-dd hh:mm)");
        int max = Console.askInt("Maximum marks");
        long id = svc.assignments.create(user, course, title, desc, due, max);
        Console.ok("Assignment created (ID " + id + ").");
    }

    private void listManaged() {
        Console.title("Assignments");
        Console.table(ASSIGN_MANAGE_COLS, svc.assignments.listManaged(user));
    }

    private void gradeSubmissions() {
        Console.title("Submissions and grading");
        Console.table(ASSIGN_MANAGE_COLS, svc.assignments.listManaged(user));
        int aid = Console.askInt("Assignment ID");
        List<String[]> subs = svc.assignments.submissions(user, aid);
        Console.table(SUBMISSION_COLS, subs);
        if (subs.isEmpty()) return;
        String action = Console.ask("G = grade a submission, D = download a submission file, Enter = back").toUpperCase();
        if (action.equals("G")) {
            int sid = Console.askInt("Submission ID");
            int marks = Console.askInt("Marks");
            String fb = Console.ask("Feedback (optional)");
            svc.assignments.grade(user, sid, marks, fb);
            Console.ok("Graded.");
        } else if (action.equals("D")) {
            int sid = Console.askInt("Submission ID");
            Console.ok("Saved to: " + svc.assignments.downloadSubmission(user, sid));
        }
    }

    private void deleteAssignment() {
        Console.title("Delete an assignment");
        Console.table(ASSIGN_MANAGE_COLS, svc.assignments.listManaged(user));
        int id = Console.askInt("Assignment ID to delete");
        if (!Console.confirm("Delete assignment " + id + " and all its submissions?")) { Console.info("Cancelled."); return; }
        svc.assignments.delete(user, id);
        Console.ok("Assignment deleted.");
    }

    // ---------------------------------------------------------------- assignments (student)

    private void myAssignments() {
        Console.title("My assignments");
        Console.table(ASSIGN_STUDENT_COLS, svc.assignments.listForStudent(user));
    }

    private void submitAssignment() {
        Console.title("Submit an assignment");
        Console.table(ASSIGN_STUDENT_COLS, svc.assignments.listForStudent(user));
        int id = Console.askInt("Assignment ID");
        String path = Console.askRequired("Full path of your file (max 10 MB)");
        svc.assignments.submit(user, id, path);
        Console.ok("Submitted. Before the deadline you may submit again to replace the file.");
    }

    // ---------------------------------------------------------------- feedback

    private void rateFaculty() {
        Console.title("Rate a faculty member");
        Console.table(new String[]{"Faculty ID", "Faculty", "Course ID", "Code", "Course"}, svc.feedback.ratableOptions(user));
        int fid = Console.askInt("Faculty ID");
        int cid = Console.askInt("Course ID");
        int rating = Console.askInt("Rating (1 = poor ... 5 = excellent)");
        String comments = Console.ask("Comments (optional, shown anonymously)");
        svc.feedback.submit(user, fid, cid, rating, comments);
        Console.ok("Thank you! Rating again for the same faculty and course updates your earlier rating.");
    }

    private void myFeedback() {
        Console.title("My feedback");
        Console.table(new String[]{"Faculty", "Course", "Rating", "Comments"}, svc.feedback.myFeedback(user));
    }

    private void ownFeedback() {
        Console.title("My feedback summary");
        Console.table(AVG_COLS, svc.feedback.averagesOwn(user));
        Console.info("Anonymous comments:");
        Console.table(new String[]{"Course", "Rating", "Comment"}, svc.feedback.commentsOwn(user));
    }

    private void allFeedback() {
        Console.title("Faculty feedback");
        Console.table(AVG_COLS, svc.feedback.averagesAll(user));
        Console.info("Anonymous comments (student names are never shown):");
        Console.table(new String[]{"Faculty", "Course", "Rating", "Comment"}, svc.feedback.commentsAll(user));
    }
}
