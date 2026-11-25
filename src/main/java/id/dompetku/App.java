package id.dompetku;

import id.dompetku.config.DatabaseConnection;
import id.dompetku.ui.LoginFrame;
import id.dompetku.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Titik awal aplikasi desktop DompetKu.
 * Mengatur tampilan global, menyiapkan koneksi database, lalu membuka layar login.
 */
public class App {
    // [OOP - Association]: Kelas App berinteraksi dengan DatabaseConnection, Theme, dan LoginFrame untuk memulai aplikasi.
    public static void main(String[] args) {
        DatabaseConnection.warmUp();
        // [OOP - Polymorphism]: Lambda expression mengimplementasikan Functional Interface Runnable untuk SwingUtilities.invokeLater.
        SwingUtilities.invokeLater(() -> {
            Theme.setup();
            new LoginFrame().setVisible(true);
        });
    }
}
