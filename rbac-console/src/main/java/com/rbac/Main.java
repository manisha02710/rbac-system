package com.rbac;

import com.rbac.exception.InvalidCredentialsException;
import com.rbac.model.BaseUser;
import com.rbac.repository.DatabaseInitializer;
import com.rbac.repository.Jdbc;
import com.rbac.service.Services;
import com.rbac.ui.RoleMenu;
import com.rbac.util.Console;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Entry point of the console application. */
public class Main {
    public static void main(String[] args) {
        Properties cfg = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            cfg.load(in);
        } catch (IOException e) {
            Console.error("db.properties not found in the current folder (" + System.getProperty("user.dir") + ").");
            Console.info("Copy db.properties next to run.bat / pom.xml and run the program from that folder.");
            return;
        }
        Jdbc.configure(cfg);

        try {
            DatabaseInitializer.run();
        } catch (RuntimeException e) {
            Console.error("Could not prepare the database: " + e.getMessage());
            if (String.valueOf(e.getMessage()).contains("No suitable driver"))
                Console.info("The MySQL driver is missing: put mysql-connector-j-8.x.jar in the lib folder (or run with Maven).");
            else
                Console.info("Check that MySQL is running and that db.user / db.password in db.properties are correct.");
            return;
        }

        Services svc = new Services();
        Console.title("RBAC & STUDENT ACADEMIC MANAGEMENT SYSTEM  (Console Edition)");
        try {
            BaseUser user = loginLoop(svc);
            if (user == null) { Console.info("Too many failed attempts. Goodbye."); return; }
            Console.ok("Welcome, " + user.getFullName() + "!  Role: " + user.getRoleLabel());
            RoleMenu.forUser(svc, user).run();
            svc.auth.logout(user);
            Console.info("Logged out. Goodbye!");
        } catch (IllegalStateException e) {
            Console.info("\nInput closed. Goodbye!");
        }
    }

    private static BaseUser loginLoop(Services svc) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = Console.askRequired("Username");
            String password = Console.askPassword("Password");
            try {
                return svc.auth.login(username, password);
            } catch (InvalidCredentialsException e) {
                Console.error(e.getMessage() + " (attempt " + attempt + " of 3)");
            } catch (RuntimeException e) {
                Console.error(e.getMessage());
            }
        }
        return null;
    }
}
