package id.dompetku.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Satu pintu untuk membuka koneksi PostgreSQL.
 *
 * Sebelumnya setiap query membuka koneksi TCP + autentikasi baru (puluhan hingga ratusan ms),
 * dan itu dilakukan di thread UI sehingga aplikasi terasa lambat. Sekarang koneksi dipakai ulang
 * lewat pool kecil: close() pada koneksi yang dipinjam hanya mengembalikannya ke pool,
 * jadi seluruh kode DAO yang memakai try-with-resources tetap berjalan tanpa perubahan.
 */
public final class DatabaseConnection {

 private static final int POOL_SIZE = 4;
 private static final BlockingQueue<Connection> IDLE = new ArrayBlockingQueue<>(POOL_SIZE);

 private DatabaseConnection() {
 }

 /** Panggil sekali saat aplikasi mulai (di thread latar) agar login & dashboard terasa instan. */
 public static void warmUp() {
  Thread t = new Thread(() -> {
   try (Connection c = getConnection()) {
    c.isValid(2);
   } catch (Exception ignored) {
    // kegagalan akan dilaporkan ke pengguna saat koneksi benar-benar dibutuhkan
   }
  }, "db-warmup");
  t.setDaemon(true);
  t.start();
 }

 public static Connection getConnection() throws SQLException {
  Connection raw;
  while ((raw = IDLE.poll()) != null) {
   try {
    if (!raw.isClosed() && raw.isValid(1)) {
     return wrap(raw);
    }
   } catch (SQLException ignored) {
    // koneksi mati, buang dan coba yang berikutnya
   }
   closeQuietly(raw);
  }
  return wrap(open());
 }

 private static Connection open() throws SQLException {
  String url = env(
          "DOMPETKU_DB_URL",
          "jdbc:postgresql://localhost:5432/dompetku"
  );

  String user = env(
          "DOMPETKU_DB_USER",
          "postgres"
  );

  String pass = env(
          "DOMPETKU_DB_PASSWORD",
          "e"
  );

  try {
   return DriverManager.getConnection(url, user, pass);
  } catch (SQLException e) {
   System.out.println("URL database: " + url);
   System.out.println("Username database: " + user);
   System.out.println("Password terisi: " + !pass.isBlank());
   e.printStackTrace();
   throw e;
  }
 }

 /** Bungkus koneksi: close() => kembalikan ke pool dalam keadaan bersih. */
 private static Connection wrap(Connection raw) {
  final boolean[] returned = {false};
  InvocationHandler h = (proxy, method, args) -> {
   String name = method.getName();

   if (name.equals("close")) {
    if (returned[0]) return null;
    returned[0] = true;
    release(raw);
    return null;
   }
   if (name.equals("isClosed")) {
    return returned[0] || raw.isClosed();
   }
   if (returned[0]) {
    throw new SQLException("Koneksi sudah ditutup.");
   }
   try {
    return method.invoke(raw, args);
   } catch (InvocationTargetException e) {
    throw e.getCause();
   }
  };
  return (Connection) Proxy.newProxyInstance(
          DatabaseConnection.class.getClassLoader(),
          new Class<?>[]{Connection.class},
          h);
 }

 private static void release(Connection raw) {
  try {
   if (raw.isClosed()) return;
   if (!raw.getAutoCommit()) {
    raw.rollback();          // transaksi yang belum di-commit dibatalkan
    raw.setAutoCommit(true); // kembalikan ke keadaan standar untuk peminjam berikutnya
   }
   if (!IDLE.offer(raw)) {
    closeQuietly(raw);
   }
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

  if (value == null || value.isBlank()) {
   return defaultValue;
  }

  return value;
 }
}
