package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.service.Services;

import java.util.ArrayList;
import java.util.List;

/** Menu for student accounts. */
public class StudentMenu extends RoleMenu {
    public StudentMenu(Services svc, BaseUser user) { super(svc, user); }

    @Override
    protected String title() { return "STUDENT MENU"; }

    @Override
    protected List<MenuItem> items() {
        List<MenuItem> l = new ArrayList<>();
        AcademicActions a = new AcademicActions(svc, user);
        l.add(profileItem());
        l.addAll(a.studentItems());
        ContentActions c = new ContentActions(svc, user);
        l.addAll(c.materialViewItems());
        l.addAll(c.assignmentStudentItems());
        l.addAll(c.feedbackStudentItems());
        l.addAll(a.announcementViewItems());
        return l;
    }
}
