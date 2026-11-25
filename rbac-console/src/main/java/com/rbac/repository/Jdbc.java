package com.rbac.repository;

import com.rbac.exception.DataAccessException;
import com.rbac.exception.DuplicateResourceException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Tiny JDBC helper: prepared statements everywhere, transactions, friendly SQL error messages. */
public final class Jdbc {
    public interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
    public interface TxWork<T> { T run(); }

    private static String url, serverUrl, user, pass, dbName;
    private static final ThreadLocal<Connection> TX = new ThreadLocal<>();
    private Jdbc() { }

    public static void configure(Properties p) {
        String host = p.getProperty("db.host", "localhost");
        String port = p.getProperty("db.port", "3306");
        dbName = p.getProperty("db.name", "rbac_console_db");
        user = p.getProperty("db.user", "root");
        pass = p.getProperty("db.password", "");
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        serverUrl = "jdbc:mysql://" + host + ":" + port + "/" + opts;
        url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + opts;
    }

    public static String dbName() { return dbName; }

    /** Connection to the MySQL server itself (no database selected) - used to create the database. */
    public static Connection openServer() throws SQLException {
        return DriverManager.getConnection(serverUrl, user, pass);
    }

    private static Connection conn() throws SQLException {
        Connection c = TX.get();
        return c != null ? c : DriverManager.getConnection(url, user, pass);
    }

    private static void release(Connection c) {
        try { if (c != null && TX.get() == null) c.close(); } catch (SQLException ignored) { }
    }

    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    public static <T> List<T> query(String sql, RowMapper<T> m, Object... args) {
        Connection c = null;
        try {
            c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, args);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) out.add(m.map(rs));
                    return out;
                }
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            release(c);
        }
    }

    public static <T> Optional<T> one(String sql, RowMapper<T> m, Object... args) {
        List<T> l = query(sql, m, args);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public static long count(String sql, Object... args) {
        return one(sql, rs -> rs.getLong(1), args).orElse(0L);
    }

    /** Runs a SELECT and returns every column as text - handy for printing tables. */
    public static List<String[]> rows(String sql, Object... args) {
        return query(sql, rs -> {
            int n = rs.getMetaData().getColumnCount();
            String[] r = new String[n];
            for (int i = 0; i < n; i++) r[i] = rs.getString(i + 1);
            return r;
        }, args);
    }

    public static int update(String sql, Object... args) {
        Connection c = null;
        try {
            c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, args);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            release(c);
        }
    }

    /** INSERT and return the generated auto-increment id. */
    public static long insert(String sql, Object... args) {
        Connection c = null;
        try {
            c = conn();
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, args);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getLong(1) : -1; }
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            release(c);
        }
    }

    /** All-or-nothing: everything inside work runs in ONE transaction (commit, or rollback on any error). */
    public static <T> T transaction(TxWork<T> work) {
        if (TX.get() != null) return work.run();           // already inside a transaction
        Connection c = null;
        try {
            c = DriverManager.getConnection(url, user, pass);
            c.setAutoCommit(false);
            TX.set(c);
            T result = work.run();
            c.commit();
            return result;
        } catch (SQLException e) {
            rollback(c);
            throw translate(e);
        } catch (RuntimeException e) {
            rollback(c);
            throw e;
        } finally {
            TX.remove();
            try { if (c != null) c.close(); } catch (SQLException ignored) { }
        }
    }

    private static void rollback(Connection c) {
        try { if (c != null) c.rollback(); } catch (SQLException ignored) { }
    }

    public static RuntimeException translate(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062: return new DuplicateResourceException("That record already exists.");
            case 1451: return new DataAccessException("Cannot delete: other records still depend on it.", e);
            case 1452: return new DataAccessException("The referenced record does not exist.", e);
            case 1045: return new DataAccessException("MySQL login failed - check db.user and db.password in db.properties.", e);
            default:   return new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }
}
