package com.rbac.util;

import java.util.List;
import java.util.Scanner;

/** Small helper for reading input and printing neat console output. */
public final class Console {
    private static final Scanner IN = new Scanner(System.in);
    private Console() { }

    public static String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!IN.hasNextLine()) throw new IllegalStateException("Input closed.");
        return IN.nextLine().trim();
    }

    public static String askRequired(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (!s.isEmpty()) return s;
            error("This field cannot be empty.");
        }
    }

    public static int askInt(String prompt) {
        while (true) {
            String s = ask(prompt);
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { error("Please enter a number."); }
        }
    }

    public static String askPassword(String prompt) {
        java.io.Console c = System.console();
        if (c != null) {
            char[] pw = c.readPassword(prompt + ": ");
            return pw == null ? "" : new String(pw);
        }
        return ask(prompt + " (visible)");
    }

    public static boolean confirm(String prompt) {
        String s = ask(prompt + " (y/n)").toLowerCase();
        return s.equals("y") || s.equals("yes");
    }

    public static void title(String t) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  " + t);
        System.out.println("=".repeat(60));
    }

    public static void info(String m)  { System.out.println(m); }
    public static void ok(String m)    { System.out.println("[OK]    " + m); }
    public static void error(String m) { System.out.println("[ERROR] " + m); }

    public static void table(String[] headers, List<String[]> rows) {
        if (rows.isEmpty()) { info("(no records)"); return; }
        int[] w = new int[headers.length];
        for (int i = 0; i < headers.length; i++) w[i] = headers[i].length();
        for (String[] r : rows)
            for (int i = 0; i < r.length && i < w.length; i++) w[i] = Math.max(w[i], r[i] == null ? 4 : r[i].length());
        StringBuilder sep = new StringBuilder("+");
        for (int x : w) sep.append("-".repeat(x + 2)).append("+");
        System.out.println(sep);
        printRow(headers, w);
        System.out.println(sep);
        for (String[] r : rows) printRow(r, w);
        System.out.println(sep);
        info(rows.size() + " row(s)");
    }

    private static void printRow(String[] r, int[] w) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < w.length; i++) {
            String v = i < r.length && r[i] != null ? r[i] : "";
            sb.append(' ').append(v).append(" ".repeat(w[i] - v.length())).append(" |");
        }
        System.out.println(sb);
    }
}
