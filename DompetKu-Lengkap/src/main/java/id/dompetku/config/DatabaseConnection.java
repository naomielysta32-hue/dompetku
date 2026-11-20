
package id.dompetku.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Satu pintu untuk membuka koneksi PostgreSQL.
 */
public final class DatabaseConnection {

 private DatabaseConnection() {
 }

 public static Connection getConnection() throws SQLException {
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

 private static String env(String key, String defaultValue) {
  String value = System.getenv(key);

  if (value == null || value.isBlank()) {
   return defaultValue;
  }

  return value;
 }
}
