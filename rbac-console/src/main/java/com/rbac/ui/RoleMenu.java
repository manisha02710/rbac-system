package com.rbac.ui;

import com.rbac.exception.AccessDeniedException;
import com.rbac.model.BaseUser;
import com.rbac.service.Services;
import com.rbac.util.Console;

import java.util.List;
import java.util.function.BiFunction;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract menu. Subclasses supply their own title and items (polymorphism);
 * the base class builds the menu from the user's DATABASE permissions and runs the loop.
 */
public abstract class RoleMenu {
    protected final Services svc;
    protected final BaseUser user;

    protected RoleMenu(Services svc, BaseUser user) {
        this.svc = svc;
        this.user = user;
    }

    protected abstract String title();

    protected abstract List<MenuItem> items();

    private static final Map<String, BiFunction<Services, BaseUser, RoleMenu>> REGISTRY = Map.of(
        "Administrator", AdminMenu::new,
        "Faculty", FacultyMenu::new,
        "Student", StudentMenu::new);

    /** Picks the menu class that matches the user's role label; unknown (custom) roles get CustomRoleMenu. */
    public static RoleMenu forUser(Services svc, BaseUser user) {
        return REGISTRY.getOrDefault(user.getRoleLabel(), CustomRoleMenu::new).apply(svc, user);
    }

    public final void run() {
        while (true) {
            List<MenuItem> visible = items().stream().filter(i -> user.can(i.permission())).collect(Collectors.toList());
            Console.title(title() + "  |  " + user.describe());
            for (int i = 0; i < visible.size(); i++) Console.info(String.format("%2d) %s", i + 1, visible.get(i).label()));
            Console.info(" 0) Logout");
            int choice = Console.askInt("Choose an option");
            if (choice == 0) return;
            if (choice < 1 || choice > visible.size()) { Console.error("Invalid option."); continue; }
            try {
                visible.get(choice - 1).action().run();
            } catch (AccessDeniedException e) {
                svc.users.denied(user, e);
                Console.error("ACCESS DENIED - " + e.getMessage());
            } catch (IllegalStateException e) {
                throw e;                                   // input closed: leave the program
            } catch (RuntimeException e) {
                Console.error(e.getMessage());
            }
            Console.ask("Press Enter to continue");
        }
    }

    protected MenuItem profileItem() {
        return new MenuItem("My profile", "OWN_PROFILE_VIEW", () -> {
            Console.title("My profile");
            Console.table(new String[]{"ID", "Username", "Name", "Email", "Roll no", "Department"}, svc.auth.profile(user));
            Console.info("Roles: " + String.join(", ", user.getRoles()));
            Console.info("Permissions (" + user.getPermissions().size() + "): " + String.join(", ", user.getPermissions()));
        });
    }
}
