package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.service.Services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** For roles created at runtime: offers every feature, but only those the role's permissions allow. */
public class CustomRoleMenu extends RoleMenu {
    public CustomRoleMenu(Services svc, BaseUser user) { super(svc, user); }

    @Override
    protected String title() { return "CUSTOM ROLE MENU"; }

    @Override
    protected List<MenuItem> items() {
        List<MenuItem> all = new ArrayList<>();
        all.add(profileItem());
        all.addAll(new AdminMenu(svc, user).items());
        all.addAll(new FacultyMenu(svc, user).items());
        all.addAll(new StudentMenu(svc, user).items());
        // the same feature can come from several menus: keep the first entry of each label
        Map<String, MenuItem> unique = new LinkedHashMap<>();
        for (MenuItem m : all) unique.putIfAbsent(m.label(), m);
        return new ArrayList<>(unique.values());
    }
}
