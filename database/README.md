# ER‑Diagram Description & Sample Queries

## ER‑Diagram (textual description)

```
[users] 1───* [user_roles] *───1 [roles]
[roles] 1───* [role_permissions] *───1 [permissions]
[users] 1───0..1 [students] 1───* [marks]
[users] 1───0..1 [students] 1───* [attendance]
[users] 1───* [audit_logs]
```

- **users**: primary entity. Holds login credentials (username, password_hash) and profile info.
- **roles**: each role groups a set of permissions.
- **permissions**: atomic actions (e.g., `USER_CREATE`). Linked to roles via **role_permissions**.
- **user_roles**: many‑to‑many link between users and roles.
- **role_permissions**: many‑to‑many link between roles and permissions.
- **students**: one‑to‑one extension of a user for student‑specific data (roll_no, department).
- **marks** & **attendance**: one‑to‑many from **students**.
- **audit_logs**: records actions performed by a user (nullable user_id for system actions).

## Useful JOIN Queries

1. **All permissions granted to a given user**
   ```sql
   SELECT p.permission_name
   FROM users u
   JOIN user_roles ur   ON u.user_id = ur.user_id
   JOIN roles r         ON ur.role_id = r.role_id
   JOIN role_permissions rp ON r.role_id = rp.role_id
   JOIN permissions p   ON rp.permission_id = p.permission_id
   WHERE u.username = 'faculty';
   ```

2. **List students with their assigned roll number, department, and the user’s full name**
   ```sql
   SELECT s.student_id, s.roll_no, s.department, u.full_name, u.email
   FROM students s
   JOIN users u ON s.user_id = u.user_id;
   ```

3. **Get a student’s marks across all subjects**
   ```sql
   SELECT s.roll_no, m.subject, m.marks
   FROM marks m
   JOIN students s ON m.student_id = s.student_id
   WHERE s.roll_no = '2023001';
   ```

4. **Attendance summary for each student (percentage attended)**
   ```sql
   SELECT s.roll_no,
          a.subject,
          a.total_classes,
          a.attended_classes,
          ROUND(100.0 * a.attended_classes / a.total_classes, 2) AS attendance_pct
   FROM attendance a
   JOIN students s ON a.student_id = s.student_id;
   ```

5. **Audit log of actions performed by admins only**
   ```sql
   SELECT al.log_id, u.username, al.action, al.status, al.created_at
   FROM audit_logs al
   LEFT JOIN users u ON al.user_id = u.user_id
   JOIN user_roles ur ON u.user_id = ur.user_id
   JOIN roles r ON ur.role_id = r.role_id
   WHERE r.role_name = 'ADMIN'
   ORDER BY al.created_at DESC;
   ```

---

### How to run & verify
1. **Create the database**
   ```powershell
   mysql -u root -p < "C:\Users\V.MANISHA\Downloads\New folder\database\schema.sql"
   ```
2. **Load seed data**
   ```powershell
   mysql -u root -p rbac_db < "C:\Users\V.MANISHA\Downloads\New folder\database\seed.sql"
   ```
3. **Test a query** (e.g., permissions of faculty) by connecting to MySQL and executing the first query shown above.

If the tables and data appear without errors, the step is complete.
