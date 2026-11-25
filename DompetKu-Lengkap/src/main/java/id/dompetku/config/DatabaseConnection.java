package id.dompetku.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Satu pintu untuk membuka dan menggunakan ulang koneksi PostgreSQL melalui connection pool sederhana.
 */
// [OOP - Encapsulation]: Constructor private mencegah instansiasi, sementara pool koneksi (IDLE) dan kredensial disembunyikan di dalam kelas.
public final class DatabaseConnection {

    private static final int POOL_SIZE = 4;
    private static final BlockingQueue<Connection> IDLE = new ArrayBlockingQueue<>(POOL_SIZE);

    private DatabaseConnection() {}

    /** Panggil sekali saat aplikasi mulai (di thread latar) agar login & dashboard terasa instan. */
    public static void warmUp() {
        Thread t = new Thread(() -> {
            // [OOP - Encapsulation]: try-with-resources otomatis memanggil close() pada proxy untuk mengembalikan koneksi ke pool.
            try (Connection c = getConnection()) {
                c.isValid(2);
            } catch (Exception ignored) {
            }
        }, "db-warmup");
        t.setDaemon(true);
        t.start();
    }

    // [OOP - Abstraction]: Pemanggil cukup memakai interface java.sql.Connection tanpa perlu mengetahui detail pooling.
    public static Connection getConnection() throws SQLException {
        Connection raw;
        while ((raw = IDLE.poll()) != null) {
            try {
                if (!raw.isClosed() && raw.isValid(1)) return wrap(raw);
            } catch (SQLException ignored) {
            }
            closeQuietly(raw);
        }
        return wrap(open());
    }

    private static Connection open() throws SQLException {
        String url = env("DOMPETKU_DB_URL", "jdbc:postgresql://localhost:5432/dompetku");
        String user = env("DOMPETKU_DB_USER", "postgres");
        String pass = env("DOMPETKU_DB_PASSWORD", "p");
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            System.out.println("URL database: " + url + " | User: " + user + " | Password terisi: " + !pass.isBlank());
            e.printStackTrace();
            throw e;
        }
    }

    /** Bungkus koneksi dengan Dynamic Proxy agar close() mengembalikan koneksi ke pool dalam keadaan bersih. */
    // [OOP - Polymorphism]: Proxy mengimplementasikan interface Connection secara dinamis dengan InvocationHandler berbasis lambda.
    private static Connection wrap(Connection raw) {
        final boolean[] returned = {false};
        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "close" -> {
                            if (!returned[0]) {
                                returned[0] = true;
                                release(raw);
                            }
                            return null;
                        }
                        case "isClosed" -> {
                            return returned[0] || raw.isClosed();
                        }
                        default -> {
                            if (returned[0]) throw new SQLException("Koneksi sudah ditutup.");
                            try {
                                return method.invoke(raw, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    }
                });
    }

    private static void release(Connection raw) {
        try {
            if (raw.isClosed()) return;
            if (!raw.getAutoCommit()) {
                raw.rollback();
                raw.setAutoCommit(true);
            }
            if (!IDLE.offer(raw)) closeQuietly(raw);
        } catch (SQLException e) {
            closeQuietly(raw);
        }
    }

    private static void closeQuietly(Connection c) {
        try {
            c.close();
        } catch (SQLException ignored) {
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
