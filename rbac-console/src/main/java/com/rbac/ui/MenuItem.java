package com.rbac.ui;

/** One menu entry. It is only shown if the user holds the permission (null = always shown). */
public record MenuItem(String label, String permission, Runnable action) { }
