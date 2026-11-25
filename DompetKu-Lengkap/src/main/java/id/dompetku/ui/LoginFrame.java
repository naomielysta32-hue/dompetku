package id.dompetku.ui;

import id.dompetku.dao.PenggunaDAO;
import id.dompetku.model.Pengguna;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

/**
 * Jendela login, registrasi akun baru, dan pemulihan kata sandi.
 */
// [OOP - Inheritance]: LoginFrame mewarisi javax.swing.JFrame sebagai jendela utama autentikasi.
public class LoginFrame extends JFrame {

    private static final int MAKS_GAGAL = 3, COOLDOWN_DETIK = 30;
    private static int gagalBeruntun = 0;
    private static long terkunciSampai = 0L;

    private final JButton masuk = Theme.buttonDark("Masuk");
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private Timer cooldownTimer;

    public LoginFrame() {
        super("DompetKu • Masuk");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 480);
        setMinimumSize(new Dimension(420, 440));
        setLocationRelativeTo(null);

        JPanel page = Theme.page();
        page.setBorder(new EmptyBorder(22, 24, 22, 24));
        page.setLayout(new BorderLayout(0, 18));

        // Header gelap + garis aksen emas
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBackground(Theme.CHARCOAL);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.add(Theme.label("DompetKu", Font.BOLD, 28, Theme.WHITE));
        titleArea.add(Box.createVerticalStrut(7));
        titleArea.add(Theme.label("Kelola keuangan mahasiswa dengan lebih teratur.", Font.PLAIN, 12, Theme.WHITE));
        header.add(titleArea, BorderLayout.CENTER);

        JPanel goldLine = new JPanel();
        goldLine.setBackground(Theme.GOLD);
        goldLine.setPreferredSize(new Dimension(1, 4));
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(header, BorderLayout.CENTER);
        headerWrapper.add(goldLine, BorderLayout.SOUTH);

        // Form login
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        Theme.styleInput(username);
        JPanel passwordWrapper = Theme.passwordWithEye(password, false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.insets = new Insets(5, 0, 5, 0);
        form.add(Theme.label("Username", Font.BOLD, 13, Theme.CHARCOAL), gbc);
        gbc.gridy = 1;
        form.add(username, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(14, 0, 5, 0);
        form.add(Theme.label("Password", Font.BOLD, 13, Theme.CHARCOAL), gbc);
        gbc.gridy = 3; gbc.insets = new Insets(5, 0, 5, 0);
        form.add(passwordWrapper, gbc);

        JButton lupa = new JButton("<html><u>Lupa password?</u></html>");
        lupa.setFont(Theme.font(Font.PLAIN, 12));
        lupa.setForeground(Theme.CHARCOAL);
        lupa.setBorderPainted(false); lupa.setContentAreaFilled(false);
        lupa.setFocusPainted(false); lupa.setOpaque(false);
        lupa.setMargin(new Insets(0, 0, 0, 0));
        lupa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lupa.addActionListener(e -> lupaPassword());
        gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(2, 0, 0, 0);
        form.add(lupa, gbc);

        JButton daftar = Theme.button("Buat akun baru");
        masuk.addActionListener(e -> login());
        daftar.addActionListener(e -> register());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actions.setOpaque(false);
        actions.add(masuk);
        actions.add(daftar);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(headerWrapper, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);

        page.add(content, BorderLayout.CENTER);
        setContentPane(page);
        getRootPane().setDefaultButton(masuk);
        if (sisaDetik() > 0) mulaiCooldown();
    }

    private static int sisaDetik() {
        long sisa = terkunciSampai - System.currentTimeMillis();
        return sisa <= 0 ? 0 : (int) Math.ceil(sisa / 1000.0);
    }

    private void mulaiCooldown() {
        masuk.setEnabled(false);
        masuk.setText("Tunggu " + sisaDetik() + " dtk");
        if (cooldownTimer != null) cooldownTimer.stop();
        cooldownTimer = new Timer(250, e -> {
            int sisa = sisaDetik();
            if (sisa <= 0) {
                ((Timer) e.getSource()).stop();
                gagalBeruntun = 0; terkunciSampai = 0L;
                masuk.setText("Masuk");
                masuk.setEnabled(true);
                password.requestFocusInWindow();
            } else {
                masuk.setText("Tunggu " + sisa + " dtk");
            }
        });
        cooldownTimer.start();
    }

    // [OOP - Polymorphism]: Method overriding dispose() memastikan timer cooldown berhenti saat jendela ditutup.
    @Override
    public void dispose() {
        if (cooldownTimer != null) cooldownTimer.stop();
        super.dispose();
    }

    private void login() {
        if (sisaDetik() > 0) return;
        String uText = username.getText().trim();
        String pText = new String(password.getPassword());
        if (uText.isEmpty() || pText.isEmpty()) {
            Dialogs.showMessageDialog(this, "Username dan password tidak boleh kosong.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String oldText = masuk.getText();
        masuk.setText("Memeriksa...");
        masuk.setEnabled(false);

        // [OOP - Association]: SwingWorker mendelegasikan autentikasi ke PenggunaDAO di thread latar.
        new SwingWorker<Optional<Pengguna>, Void>() {
            @Override protected Optional<Pengguna> doInBackground() throws Exception {
                return new PenggunaDAO().login(uText, pText);
            }

            @Override protected void done() {
                masuk.setText(oldText);
                masuk.setEnabled(true);
                try {
                    Optional<Pengguna> userOpt = get();
                    if (userOpt.isEmpty()) {
                        gagalBeruntun++;
                        password.setText("");
                        if (gagalBeruntun >= MAKS_GAGAL) {
                            terkunciSampai = System.currentTimeMillis() + COOLDOWN_DETIK * 1000L;
                            mulaiCooldown();
                            Dialogs.showMessageDialog(LoginFrame.this,
                                    "Anda salah memasukkan username atau password " + MAKS_GAGAL + " kali. Coba lagi setelah " + COOLDOWN_DETIK + " detik.",
                                    "Terlalu Banyak Percobaan", JOptionPane.WARNING_MESSAGE);
                        } else {
                            Dialogs.showMessageDialog(LoginFrame.this,
                                    "Username atau password salah. Sisa percobaan: " + (MAKS_GAGAL - gagalBeruntun) + ".",
                                    "Gagal Masuk", JOptionPane.WARNING_MESSAGE);
                        }
                        return;
                    }
                    gagalBeruntun = 0; terkunciSampai = 0L;
                    new DashboardFrame(userOpt.get()).setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    cause.printStackTrace();
                    showError(cause instanceof Exception e ? e : ex);
                }
            }
        }.execute();
    }

    private void register() {
        JTextField nama = new JTextField(), user = new JTextField();
        JPasswordField pass = new JPasswordField();
        Theme.filterNoDigits(nama);
        JPanel passWrapper = Theme.passwordWithEye(pass, false);

        Object[] fields = {"Nama lengkap", nama, "Username (minimal 3 karakter)", user,
                "Password (min. 8 karakter, huruf & angka)", passWrapper};
        if (Dialogs.showConfirmDialog(this, fields, "Registrasi DompetKu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        try {
            String namaText = nama.getText().trim(), passText = new String(pass.getPassword());
            if (namaText.matches(".*\\d.*")) throw new IllegalArgumentException("Nama lengkap tidak boleh mengandung angka.");
            if (!passText.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                throw new IllegalArgumentException("Password harus mengandung kombinasi huruf dan angka.");
            }
            Pengguna p = new PenggunaDAO().daftar(namaText, user.getText(), passText);
            Dialogs.showMessageDialog(this, "Akun berhasil dibuat. Silakan masuk.", "Registrasi berhasil", JOptionPane.INFORMATION_MESSAGE);
            username.setText(p.getUsername());
            password.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError(ex);
        }
    }

    private void lupaPassword() {
        PenggunaDAO dao = new PenggunaDAO();
        JTextField user = new JTextField(username.getText().trim()), nama = new JTextField();
        Theme.filterNoDigits(nama);
        Object[] identitas = {"Username", user, "Nama lengkap (sesuai saat daftar)", nama};

        final int maxCoba = 3;
        boolean cocok = false;
        for (int coba = 1; coba <= maxCoba && !cocok; coba++) {
            if (Dialogs.showConfirmDialog(this, identitas, "Lupa Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                cocok = dao.identitasCocok(user.getText(), nama.getText());
            } catch (Exception ex) {
                ex.printStackTrace();
                showError(ex);
                return;
            }
            if (!cocok) {
                int sisa = maxCoba - coba;
                Dialogs.showMessageDialog(this, sisa == 0 ? "Terlalu banyak percobaan. Silakan coba lagi nanti."
                                : "Username atau nama lengkap tidak sesuai.\nSisa percobaan: " + sisa,
                        "Gagal", JOptionPane.WARNING_MESSAGE);
                if (sisa == 0) return;
            }
        }
        if (!cocok) return;

        JPasswordField baru = new JPasswordField(), ulangi = new JPasswordField();
        Object[] fieldsBaru = {"Password baru (min. 8 karakter, huruf & angka)", Theme.passwordWithEye(baru, false),
                "Ulangi password baru", Theme.passwordWithEye(ulangi, false)};

        while (true) {
            if (Dialogs.showConfirmDialog(this, fieldsBaru, "Buat Password Baru",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String p1 = new String(baru.getPassword()), p2 = new String(ulangi.getPassword());
            String err = p1.length() < 8 ? "Password minimal 8 karakter."
                    : !p1.matches("^(?=.*[A-Za-z])(?=.*\\d).+$") ? "Password harus mengandung kombinasi huruf dan angka."
                    : !p1.equals(p2) ? "Password dan ulangi password tidak sama." : null;
            if (err != null) {
                Dialogs.showMessageDialog(this, err, "Peringatan", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            try {
                dao.resetPassword(user.getText(), nama.getText(), p1);
                Dialogs.showMessageDialog(this, "Password berhasil diubah. Silakan masuk dengan password baru.",
                        "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                username.setText(user.getText().trim());
                password.setText("");
                password.requestFocusInWindow();
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                showError(ex);
                return;
            }
        }
    }

    private void showError(Exception ex) {
        Dialogs.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Terjadi kesalahan", JOptionPane.ERROR_MESSAGE);
    }
}
