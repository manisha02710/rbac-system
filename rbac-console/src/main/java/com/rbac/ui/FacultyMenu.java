package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.service.Services;

import java.util.ArrayList;
import java.util.List;

/** Menu for faculty accounts. */
public class FacultyMenu extends RoleMenu {
    public FacultyMenu(Services svc, BaseUser user) { super(svc, user); }

    @Override
    protected String title() { return "FACULTY MENU"; }

    @Override
    protected List<MenuItem> items() {
        List<MenuItem> l = new ArrayList<>();
        AcademicActions a = new AcademicActions(svc, user);
        l.add(profileItem());
        l.addAll(a.viewItems());
        l.addAll(a.manageItems());
        ContentActions c = new ContentActions(svc, user);
        l.addAll(c.materialViewItems());
        l.addAll(c.materialManageItems());
        l.addAll(c.assignmentManageItems());
        l.addAll(c.feedbackFacultyItems());
        l.addAll(a.announcementViewItems());
        return l;
    }
}
