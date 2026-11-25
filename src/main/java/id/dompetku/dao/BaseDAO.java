package id.dompetku.dao;

import id.dompetku.config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Kelas induk abstrak bagi seluruh DAO untuk menghilangkan duplikasi boilerplate JDBC.
 */
// [OOP - Abstraction]: BaseDAO mengabstraksikan eksekusi query, pemetaan ResultSet, dan manajemen transaksi JDBC.
public abstract class BaseDAO {

    // [OOP - Polymorphism]: Functional interface RowMapper memungkinkan pemetaan baris ResultSet secara fleksibel lewat lambda.
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    protected interface TxWork<T> {
        T execute(Connection c) throws SQLException;
    }

    @FunctionalInterface
    protected interface TxAction {
        void execute(Connection c) throws SQLException;
    }

    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        // [OOP - Encapsulation]: Pengambilan dan penutupan koneksi dikelola otomatis lewat try-with-resources.
        try (Connection c = DatabaseConnection.getConnection()) {
            return queryList(c, sql, mapper, params);
        }
    }

    // [OOP - Polymorphism]: Method overloading queryList untuk memakai koneksi transaksi yang sudah ada.
    protected <T> List<T> queryList(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
        }
        return out;
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return queryOne(c, sql, mapper, params);
        }
    }

    // [OOP - Polymorphism]: Method overloading queryOne untuk eksekusi di dalam koneksi transaksi aktif.
    protected <T> Optional<T> queryOne(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
            }
        }
    }

    protected int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return executeUpdate(c, sql, params);
        }
    }

    // [OOP - Polymorphism]: Method overloading executeUpdate untuk perintah DML di dalam transaksi.
    protected int executeUpdate(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Menjalankan blok operasi database secara atomik (auto-commit dimatikan, commit/rollback otomatis). */
    protected <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.execute(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException s) throw s;
                if (e instanceof RuntimeException r) throw r;
                throw new SQLException(e);
            }
        }
    }

    // [OOP - Polymorphism]: Method overloading inTransaction untuk transaksi tanpa nilai kembalian.
    protected void inTransaction(TxAction action) throws SQLException {
        inTransaction(c -> {
            action.execute(c);
            return null;
        });
    }

    protected static String[] rowStrings(ResultSet rs, int cols) throws SQLException {
        String[] row = new String[cols];
        for (int i = 0; i < cols; i++) row[i] = rs.getString(i + 1);
        return row;
    }

    protected static SQLException mapUnique(SQLException e, String duplicateMsg) {
        return "23505".equals(e.getSQLState()) ? new SQLException(duplicateMsg) : e;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object v = params[i];
            if (v instanceof LocalDateTime dt) ps.setTimestamp(i + 1, Timestamp.valueOf(dt));
            else ps.setObject(i + 1, v);
        }
    }
}
