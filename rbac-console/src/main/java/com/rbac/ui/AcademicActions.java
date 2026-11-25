package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.service.AcademicService;
import com.rbac.service.CommunicationService;
import com.rbac.service.Services;
import com.rbac.util.Console;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu actions for students, marks, attendance, complaints and announcements.
 * Menus pick the groups they need; RoleMenu still hides anything the user has no permission for.
 */
public class AcademicActions {
    private final Services svc;
    private final BaseUser user;

    private static final String[] STUDENT_COLS = {"ID", "Roll no", "Name", "Department", "Email"};
    private static final String[] MARK_COLS = {"Mark ID", "Student ID", "Roll no", "Name", "Subject", "Marks", "Grade"};
    private static final String[] ATT_COLS = {"Rec ID", "Student ID", "Roll no", "Name", "Subject", "Attended", "Total", "%", "Status"};
    private static final String[] COMPLAINT_ADMIN_COLS = {"ID", "Roll no", "Student", "Subject", "Category", "Status", "Response", "Created"};

    public AcademicActions(Services svc, BaseUser user) {
        this.svc = svc;
        this.user = user;
    }

    // ---------------------------------------------------------------- item groups

    /** Read-only view of students, marks and attendance (admin and faculty). */
    public List<MenuItem> viewItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("List students", "STUDENT_READ", this::listStudents));
        l.add(new MenuItem("View marks", "MARKS_READ", this::viewMarks));
        l.add(new MenuItem("View attendance", "ATTENDANCE_VIEW", this::viewAttendance));
        l.add(new MenuItem("Low attendance report (below 75%)", "ATTENDANCE_VIEW", this::lowAttendance));
        return l;
    }

    /** Add / edit / delete marks and attendance (faculty). */
    public List<MenuItem> manageItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Add marks", "MARKS_ADD", this::addMark));
        l.add(new MenuItem("Edit marks", "MARKS_ADD", this::editMark));
        l.add(new MenuItem("Delete marks", "MARKS_ADD", this::deleteMark));
        l.add(new MenuItem("Add attendance", "ATTENDANCE_MANAGE", this::addAttendance));
        l.add(new MenuItem("Edit attendance", "ATTENDANCE_MANAGE", this::editAttendance));
        l.add(new MenuItem("Delete attendance", "ATTENDANCE_MANAGE", this::deleteAttendance));
        return l;
    }

    /** A student's own data. */
    public List<MenuItem> studentItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("My marks", "OWN_MARKS_VIEW", this::myMarks));
        l.add(new MenuItem("My attendance", "OWN_ATTENDANCE_VIEW", this::myAttendance));
        l.add(new MenuItem("Submit a complaint", "COMPLAINT_CREATE", this::submitComplaint));
        l.add(new MenuItem("My complaints", "COMPLAINT_VIEW_OWN", this::myComplaints));
        return l;
    }

    public List<MenuItem> complaintAdminItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("View all complaints", "COMPLAINT_MANAGE", this::allComplaints));
        l.add(new MenuItem("Respond to a complaint", "COMPLAINT_MANAGE", this::respondComplaint));
        return l;
    }

    public List<MenuItem> announcementViewItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("View announcements", "ANNOUNCEMENT_VIEW", this::viewAnnouncements));
        return l;
    }

    public List<MenuItem> announcementManageItems() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Post an announcement", "ANNOUNCEMENT_MANAGE", this::postAnnouncement));
        l.add(new MenuItem("Delete an announcement", "ANNOUNCEMENT_MANAGE", this::deleteAnnouncement));
        return l;
    }

    // ---------------------------------------------------------------- students, marks, attendance

    private void listStudents() {
        Console.title("Students");
        Console.table(STUDENT_COLS, svc.academic.listStudents(user));
    }

    private int pickStudent() {
        Console.table(STUDENT_COLS, svc.academic.listStudents(user));
        return Console.askInt("Student ID");
    }

    private void viewMarks() {
        Console.title("Marks");
        int id = Console.askInt("Student ID (0 = all students)");
        Console.table(MARK_COLS, svc.academic.listMarks(user, id));
    }

    private void addMark() {
        Console.title("Add marks");
        int studentId = pickStudent();
        String subject = Console.askRequired("Subject");
        int marks = Console.askInt("Marks (0-100)");
        long id = svc.academic.addMark(user, studentId, subject, marks);
        Console.ok("Marks saved (record " + id + ").");
    }

    private void editMark() {
        Console.title("Edit marks");
        Console.table(MARK_COLS, svc.academic.listMarks(user, 0));
        int id = Console.askInt("Mark ID to edit");
        int marks = Console.askInt("New marks (0-100)");
        svc.academic.updateMark(user, id, marks);
        Console.ok("Marks updated.");
    }

    private void deleteMark() {
        Console.title("Delete marks");
        Console.table(MARK_COLS, svc.academic.listMarks(user, 0));
        int id = Console.askInt("Mark ID to delete");
        if (!Console.confirm("Delete mark record " + id + "?")) { Console.info("Cancelled."); return; }
        svc.academic.deleteMark(user, id);
        Console.ok("Marks deleted.");
    }

    private void viewAttendance() {
        Console.title("Attendance");
        int id = Console.askInt("Student ID (0 = all students)");
        Console.table(ATT_COLS, svc.academic.listAttendance(user, id));
    }

    private void lowAttendance() {
        Console.title("Students with overall attendance below " + AcademicService.LOW_ATTENDANCE_PERCENT + "%");
        Console.table(new String[]{"Roll no", "Name", "Attended", "Total", "%"}, svc.academic.lowAttendanceReport(user));
    }

    private void addAttendance() {
        Console.title("Add attendance");
        int studentId = pickStudent();
        String subject = Console.askRequired("Subject");
        int total = Console.askInt("Total classes held");
        int attended = Console.askInt("Classes attended");
        long id = svc.academic.addAttendance(user, studentId, subject, total, attended);
        Console.ok("Attendance saved (record " + id + ").");
    }

    private void editAttendance() {
        Console.title("Edit attendance");
        Console.table(ATT_COLS, svc.academic.listAttendance(user, 0));
        int id = Console.askInt("Attendance record ID to edit");
        int total = Console.askInt("New total classes");
        int attended = Console.askInt("New attended classes");
        svc.academic.updateAttendance(user, id, total, attended);
        Console.ok("Attendance updated.");
    }

    private void deleteAttendance() {
        Console.title("Delete attendance");
        Console.table(ATT_COLS, svc.academic.listAttendance(user, 0));
        int id = Console.askInt("Attendance record ID to delete");
        if (!Console.confirm("Delete attendance record " + id + "?")) { Console.info("Cancelled."); return; }
        svc.academic.deleteAttendance(user, id);
        Console.ok("Attendance deleted.");
    }

    private void myMarks() {
        Console.title("My marks");
        Console.table(new String[]{"Subject", "Marks", "Grade"}, svc.academic.myMarks(user));
        Console.info("Summary: " + svc.academic.myMarksSummary(user));
    }

    private void myAttendance() {
        Console.title("My attendance");
        Console.table(new String[]{"Subject", "Attended", "Total", "%", "Status"}, svc.academic.myAttendance(user));
        Console.info("Overall: " + svc.academic.myOverallAttendance(user));
    }

    // ---------------------------------------------------------------- complaints

    private void submitComplaint() {
        Console.title("Submit a complaint");
        String subject = Console.askRequired("Subject");
        String description = Console.askRequired("Describe the problem");
        String category = Console.askRequired("Category (ACADEMIC / FACULTY / FACILITY / OTHER)");
        long id = svc.comms.submitComplaint(user, subject, description, category);
        Console.ok("Complaint submitted. Your complaint number is " + id + ".");
    }

    private void myComplaints() {
        Console.title("My complaints");
        Console.table(new String[]{"ID", "Subject", "Category", "Status", "Admin response", "Created"}, svc.comms.myComplaints(user));
    }

    private void allComplaints() {
        Console.title("All complaints");
        Console.table(COMPLAINT_ADMIN_COLS, svc.comms.allComplaints(user));
    }

    private void respondComplaint() {
        Console.title("Respond to a complaint");
        Console.table(COMPLAINT_ADMIN_COLS, svc.comms.allComplaints(user));
        int id = Console.askInt("Complaint ID");
        Console.info("Complaint text: " + svc.comms.complaintText(user, id));
        String status = Console.askRequired("New status (OPEN / IN_PROGRESS / RESOLVED)");
        String response = Console.askRequired("Your response");
        svc.comms.respondToComplaint(user, id, status, response);
        Console.ok("Complaint updated.");
    }

    // ---------------------------------------------------------------- announcements

    private void viewAnnouncements() {
        Console.title("Announcements");
        List<String[]> rows = svc.comms.activeAnnouncements(user);
        if (rows.isEmpty()) { Console.info("(no announcements)"); return; }
        for (String[] r : rows) {
            Console.info("[" + r[1] + "] " + r[2] + "   (posted " + r[5] + " by " + r[4]
                + ("-".equals(r[6]) ? "" : ", valid until " + r[6]) + ")");
            Console.info("    " + r[3]);
            Console.info("");
        }
    }

    private void postAnnouncement() {
        Console.title("Post an announcement");
        String title = Console.askRequired("Title");
        String message = Console.askRequired("Message");
        String category = Console.askRequired("Category (HOLIDAY / EXAM / EVENT / GENERAL)");
        String expiry = Console.ask("Valid until (yyyy-mm-dd, or press Enter for no expiry)");
        long id = svc.comms.postAnnouncement(user, title, message, category, expiry);
        Console.ok("Announcement posted (ID " + id + ").");
    }

    private void deleteAnnouncement() {
        Console.title("Delete an announcement");
        Console.table(new String[]{"ID", "Category", "Title", "Posted by", "Date", "Expires", "State"}, svc.comms.allAnnouncements(user));
        int id = Console.askInt("Announcement ID to delete");
        if (!Console.confirm("Delete announcement " + id + "?")) { Console.info("Cancelled."); return; }
        svc.comms.deleteAnnouncement(user, id);
        Console.ok("Announcement deleted.");
    }
}
