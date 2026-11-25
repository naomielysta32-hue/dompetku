package id.dompetku;

import id.dompetku.config.DatabaseConnection;
import id.dompetku.ui.LoginFrame;
import id.dompetku.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Entry point aplikasi desktop DompetKu.
 */
public class App {

 public static void main(String[] args) {
  // Buka koneksi database di latar belakang selagi jendela login digambar
  DatabaseConnection.warmUp();

  SwingUtilities.invokeLater(() -> {
   Theme.setup();

   LoginFrame login = new LoginFrame();
   login.setVisible(true);
  });
 }
}
