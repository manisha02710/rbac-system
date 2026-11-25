package com.rbac.ui;

import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;
import com.rbac.service.Services;
import com.rbac.util.Console;

import java.util.ArrayList;
import java.util.List;

/** Administrator menu: users, roles, permissions, student data, complaints, announcements and the audit log. */
public class AdminMenu extends RoleMenu {
    public AdminMenu(Services svc, BaseUser user) { super(svc, user); }

    @Override
    protected String title() { return "ADMIN MENU"; }

    @Override
    protected List<MenuItem> items() {
        List<MenuItem> l = new ArrayList<>();
        l.add(new MenuItem("Dashboard (system summary)", "USER_READ", this::dashboard));
        l.add(new MenuItem("List users", "USER_READ", this::listUsers));
        l.add(new MenuItem("Create user", "USER_CREATE", this::createUser));
        l.add(new MenuItem("Enable / disable a user", "USER_DISABLE", this::toggleUser));
        l.add(new MenuItem("List roles", "ROLE_ASSIGN", this::listRoles));
        l.add(new MenuItem("Assign a role to a user", "ROLE_ASSIGN", this::assignRole));
        l.add(new MenuItem("Remove a role from a user", "ROLE_ASSIGN", this::removeRole));
        l.add(new MenuItem("Create a new role", "ROLE_CREATE", this::createRole));
        l.add(new MenuItem("View a role's permissions", "PERMISSION_ASSIGN", this::viewRolePermissions));
        l.add(new MenuItem("Grant a permission to a role", "PERMISSION_ASSIGN", this::grant));
        l.add(new MenuItem("Revoke a permission from a role", "PERMISSION_ASSIGN", this::revoke));
        AcademicActions a = new AcademicActions(svc, user);
        l.addAll(a.viewItems());
        l.addAll(a.complaintAdminItems());
        l.addAll(a.announcementViewItems());
        l.addAll(a.announcementManageItems());
        ContentActions c = new ContentActions(svc, user);
        l.addAll(c.courseAdminItems());
        l.addAll(c.materialViewItems());
        l.add(new MenuItem("Delete a material", "MATERIAL_MANAGE", () -> c.materialManageItems().get(1).action().run()));
        l.addAll(c.feedbackAdminItems());
        l.add(new MenuItem("View audit log", "AUDIT_VIEW", this::audit));
        return l;
    }

    private static final String[] USER_COLS = {"ID", "Username", "Name", "Email", "Status", "Roles"};
    private static final String[] ROLE_COLS = {"ID", "Role", "Description", "Permissions"};
    private static final String[] PERM_COLS = {"ID", "Permission", "Description"};

    private void dashboard() {
        Console.title("System summary");
        Console.info("Users       : " + Jdbc.count("SELECT COUNT(*) FROM users")
                + " (" + Jdbc.count("SELECT COUNT(*) FROM users WHERE is_active") + " active)");
        Console.info("Roles       : " + Jdbc.count("SELECT COUNT(*) FROM roles"));
        Console.info("Permissions : " + Jdbc.count("SELECT COUNT(*) FROM permissions"));
        Console.info("Students    : " + Jdbc.count("SELECT COUNT(*) FROM students"));
        Console.info("Faculty     : " + Jdbc.count("SELECT COUNT(*) FROM faculty_profiles"));
        Console.info("Audit events: " + Jdbc.count("SELECT COUNT(*) FROM audit_logs"));
    }

    private void listUsers() {
        Console.title("All users");
        Console.table(USER_COLS, svc.users.list(user));
    }

    private void createUser() {
        Console.title("Create user");
        Console.table(ROLE_COLS, svc.roles.listRoles());
        int roleId = Console.askInt("Role ID for the new user");
        String username = Console.askRequired("Username");
        String fullName = Console.askRequired("Full name");
        String email = Console.askRequired("Email");
        String password = Console.askPassword("Password (min 6 characters)");
        String roll = "", dept = "";
        if (Jdbc.count("SELECT COUNT(*) FROM roles WHERE role_id = ? AND role_name = 'STUDENT'", roleId) > 0) {
            roll = Console.askRequired("Roll number");
            dept = Console.askRequired("Department");
        }
        long id = svc.users.create(user, username, password, fullName, email, roleId, roll, dept);
        Console.ok("User created with ID " + id);
    }

    private void toggleUser() {
        Console.title("Enable / disable a user");
        Console.table(USER_COLS, svc.users.list(user));
        int id = Console.askInt("User ID");
        boolean enable = Console.ask("Type E to enable or D to disable").equalsIgnoreCase("E");
        svc.users.setActive(user, id, enable);
        Console.ok("User " + id + (enable ? " enabled." : " disabled."));
    }

    private void listRoles() {
        Console.title("Roles");
        Console.table(ROLE_COLS, svc.roles.listRoles());
    }

    private void assignRole() {
        Console.title("Assign a role");
        Console.table(USER_COLS, svc.users.list(user));
        Console.table(ROLE_COLS, svc.roles.listRoles());
        svc.users.assignRole(user, Console.askInt("User ID"), Console.askInt("Role ID"));
        Console.ok("Role assigned. The user gets the new permissions at their next login.");
    }

    private void removeRole() {
        Console.title("Remove a role");
        Console.table(USER_COLS, svc.users.list(user));
        svc.users.removeRole(user, Console.askInt("User ID"), Console.askInt("Role ID to remove"));
        Console.ok("Role removed.");
    }

    private void createRole() {
        Console.title("Create a new role");
        String name = Console.askRequired("Role name (e.g. DEPARTMENT_COORDINATOR)");
        String desc = Console.ask("Description");
        long id = svc.roles.createRole(user, name, desc);
        Console.ok("Role created with ID " + id + ". Now grant it permissions.");
    }

    private void viewRolePermissions() {
        Console.table(ROLE_COLS, svc.roles.listRoles());
        int roleId = Console.askInt("Role ID");
        Console.title("Permissions of role " + roleId);
        Console.table(PERM_COLS, svc.roles.permissionsOfRole(roleId));
    }

    private void grant() {
        Console.table(ROLE_COLS, svc.roles.listRoles());
        int roleId = Console.askInt("Role ID");
        Console.table(PERM_COLS, svc.roles.allPermissions());
        svc.roles.grant(user, roleId, Console.askInt("Permission ID to grant"));
        Console.ok("Permission granted.");
    }

    private void revoke() {
        Console.table(ROLE_COLS, svc.roles.listRoles());
        int roleId = Console.askInt("Role ID");
        Console.table(PERM_COLS, svc.roles.permissionsOfRole(roleId));
        svc.roles.revoke(user, roleId, Console.askInt("Permission ID to revoke"));
        Console.ok("Permission revoked.");
    }

    private void audit() {
        Console.title("Audit log (latest 30)");
        Console.table(new String[]{"ID", "Time", "User", "Action", "Status", "Details"}, svc.audit.recent(30));
    }
}
