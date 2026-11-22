package id.dompetku;

import id.dompetku.ui.LoginFrame;
import id.dompetku.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Entry point aplikasi desktop DompetKu.
 */
public class App {

 public static void main(String[] args) {
  SwingUtilities.invokeLater(() -> {
   Theme.setup();

   LoginFrame login = new LoginFrame();
   login.setVisible(true);
  });
 }
}
