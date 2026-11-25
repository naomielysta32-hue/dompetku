package id.dompetku.ui;

import id.dompetku.dao.PenggunaDAO;
import id.dompetku.model.Pengguna;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private JButton masuk;

    // Batas percobaan login. Static supaya tetap berlaku walau jendela login dibuka ulang.
    private static final int MAKS_GAGAL = 3;
    private static final int COOLDOWN_DETIK = 30;
    private static int gagalBeruntun = 0;
    private static long terkunciSampai = 0L; // epoch millis

    private javax.swing.Timer cooldownTimer;

    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        super("DompetKu • Masuk");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 480);
        setMinimumSize(new Dimension(420, 440));
        setLocationRelativeTo(null);

        JPanel page = Theme.page();
        page.setBackground(Theme.BG);
        page.setBorder(new EmptyBorder(22, 24, 22, 24));
        page.setLayout(new BorderLayout(0, 18));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBackground(Theme.CHARCOAL);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("DompetKu");
        title.setForeground(Theme.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel subtitle = new JLabel(
                "Kelola keuangan mahasiswa dengan lebih teratur."
        );
        subtitle.setForeground(Theme.WHITE);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.add(title);
        titleArea.add(Box.createVerticalStrut(7));
        titleArea.add(subtitle);

        JPanel goldLine = new JPanel();
        goldLine.setBackground(Theme.GOLD);
        goldLine.setPreferredSize(new Dimension(1, 4));

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(header, BorderLayout.CENTER);
        headerWrapper.add(goldLine, BorderLayout.SOUTH);

        header.add(titleArea, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        JLabel usernameLabel = createLabel("Username");
        JLabel passwordLabel = createLabel("Password");

        styleInput(username);
        styleInput(password);

        gbc.gridy = 0;
        form.add(usernameLabel, gbc);

        gbc.gridy = 1;
        form.add(username, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 5, 0);
        form.add(passwordLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 5, 0);
        form.add(password, gbc);

        // Link "Lupa password?" di bawah kolom password, rata kanan
        JButton lupa = new JButton("<html><u>Lupa password?</u></html>");
        lupa.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lupa.setForeground(Theme.CHARCOAL);
        lupa.setBorderPainted(false);
        lupa.setContentAreaFilled(false);
        lupa.setFocusPainted(false);
        lupa.setOpaque(false);
        lupa.setMargin(new Insets(0, 0, 0, 0));
        lupa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lupa.addActionListener(e -> lupaPassword());

        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(2, 0, 0, 0);
        form.add(lupa, gbc);

        masuk = Theme.button("Masuk");
        masuk.setBackground(Theme.CHARCOAL);
        masuk.setForeground(Theme.WHITE);

        JButton daftar = Theme.button("Buat akun baru");

        masuk.addActionListener(e -> login());
        daftar.addActionListener(e -> register());

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 5)
        );
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

        // Jika masih dalam masa tunggu (mis. setelah logout), lanjutkan hitungan mundur
        if (sisaDetik() > 0) {
            mulaiCooldown();
        }
    }

    private static int sisaDetik() {
        long sisa = terkunciSampai - System.currentTimeMillis();
        return sisa <= 0 ? 0 : (int) Math.ceil(sisa / 1000.0);
    }

    /** Nonaktifkan tombol Masuk dan tampilkan hitungan mundur di tombol. */
    private void mulaiCooldown() {
        masuk.setEnabled(false);
        masuk.setText("Tunggu " + sisaDetik() + " dtk");

        if (cooldownTimer != null) cooldownTimer.stop();
        cooldownTimer = new javax.swing.Timer(250, e -> {
            int sisa = sisaDetik();
            if (sisa <= 0) {
                ((javax.swing.Timer) e.getSource()).stop();
                gagalBeruntun = 0;
                terkunciSampai = 0L;
                masuk.setText("Masuk");
                masuk.setEnabled(true);
                password.requestFocusInWindow();
            } else {
                masuk.setText("Tunggu " + sisa + " dtk");
            }
        });
        cooldownTimer.start();
    }

    @Override
    public void dispose() {
        if (cooldownTimer != null) cooldownTimer.stop();
        super.dispose();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(Theme.CHARCOAL);
        return label;
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(Theme.CHARCOAL);
        field.setBackground(Color.WHITE);
        field.setCaretColor(Theme.CHARCOAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(9, 10, 9, 10)
        ));
    }

    private void login() {
        if (sisaDetik() > 0) {
            return; // masih masa tunggu (mis. ditekan lewat Enter)
        }

        try {
            String uText = username.getText().trim();
            String pText = new String(password.getPassword());

            if (uText.isEmpty() || pText.isEmpty()) {
                Dialogs.showMessageDialog(
                        this,
                        "Username dan password tidak boleh kosong.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Verifikasi BCrypt + query dijalankan di thread latar agar jendela tidak membeku
            final String oldText = masuk.getText();
            masuk.setText("Memeriksa...");
            masuk.setEnabled(false);

            new SwingWorker<Optional<Pengguna>, Void>() {
                @Override
                protected Optional<Pengguna> doInBackground() throws Exception {
                    return new PenggunaDAO().login(uText, pText);
                }

                @Override
                protected void done() {
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
                                Dialogs.showMessageDialog(
                                        LoginFrame.this,
                                        "Anda salah memasukkan username atau password " + MAKS_GAGAL
                                                + " kali. Coba lagi setelah " + COOLDOWN_DETIK + " detik.",
                                        "Terlalu Banyak Percobaan",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            } else {
                                Dialogs.showMessageDialog(
                                        LoginFrame.this,
                                        "Username atau password salah. Sisa percobaan: "
                                                + (MAKS_GAGAL - gagalBeruntun) + ".",
                                        "Gagal Masuk",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            }
                            return;
                        }

                        gagalBeruntun = 0;
                        terkunciSampai = 0L;

                        new DashboardFrame(userOpt.get()).setVisible(true);
                        dispose();

                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        cause.printStackTrace();
                        showError(cause instanceof Exception ? (Exception) cause : ex);
                    }
                }
            }.execute();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError(ex);
        }
    }

    private void register() {
        JTextField nama = new JTextField();
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();

        styleInput(nama);
        styleInput(user);
        styleInput(pass);

        Object[] fields = {
                "Nama lengkap", nama,
                "Username (minimal 3 karakter)", user,
                "Password (minimal 8 karakter)", pass
        };

        if (Dialogs.showConfirmDialog(
                this,
                fields,
                "Registrasi DompetKu",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        ) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Pengguna p = new PenggunaDAO().daftar(
                    nama.getText(),
                    user.getText(),
                    new String(pass.getPassword())
            );

            Dialogs.showMessageDialog(
                    this,
                    "Akun berhasil dibuat. Silakan masuk.",
                    "Registrasi berhasil",
                    JOptionPane.INFORMATION_MESSAGE
            );

            username.setText(p.getUsername());
            password.setText("");

        } catch (Exception ex) {
            ex.printStackTrace();
            showError(ex);
        }
    }

    // ==========================================
    // LUPA PASSWORD: username + nama lengkap, lalu password baru
    // ==========================================

    private void lupaPassword() {
        PenggunaDAO dao = new PenggunaDAO();

        JTextField user = new JTextField(username.getText().trim());
        JTextField nama = new JTextField();

        styleInput(user);
        styleInput(nama);

        Object[] identitas = {
                "Username", user,
                "Nama lengkap (sesuai saat daftar)", nama
        };

        final int maxCoba = 3;
        boolean cocok = false;

        // Langkah 1: verifikasi username + nama lengkap (maksimal 3 kali coba)
        for (int coba = 1; coba <= maxCoba && !cocok; coba++) {
            if (Dialogs.showConfirmDialog(
                    this,
                    identitas,
                    "Lupa Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            ) != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                cocok = dao.identitasCocok(user.getText(), nama.getText());
            } catch (Exception ex) {
                ex.printStackTrace();
                showError(ex);
                return;
            }

            if (!cocok) {
                int sisa = maxCoba - coba;

                if (sisa == 0) {
                    Dialogs.showMessageDialog(
                            this,
                            "Terlalu banyak percobaan. Silakan coba lagi nanti.",
                            "Gagal",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                Dialogs.showMessageDialog(
                        this,
                        "Username atau nama lengkap tidak sesuai.\nSisa percobaan: " + sisa,
                        "Gagal",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }

        if (!cocok) return;

        // Langkah 2: isi password baru
        JPasswordField baru = new JPasswordField();
        JPasswordField ulangi = new JPasswordField();

        styleInput(baru);
        styleInput(ulangi);

        Object[] fieldsBaru = {
                "Password baru (minimal 8 karakter)", baru,
                "Ulangi password baru", ulangi
        };

        while (true) {
            if (Dialogs.showConfirmDialog(
                    this,
                    fieldsBaru,
                    "Buat Password Baru",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            ) != JOptionPane.OK_OPTION) {
                return;
            }

            String p1 = new String(baru.getPassword());
            String p2 = new String(ulangi.getPassword());

            if (p1.length() < 8) {
                Dialogs.showMessageDialog(
                        this,
                        "Password minimal 8 karakter.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            if (!p1.equals(p2)) {
                Dialogs.showMessageDialog(
                        this,
                        "Password dan ulangi password tidak sama.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            try {
                dao.resetPassword(user.getText(), nama.getText(), p1);

                Dialogs.showMessageDialog(
                        this,
                        "Password berhasil diubah. Silakan masuk dengan password baru.",
                        "Berhasil",
                        JOptionPane.INFORMATION_MESSAGE
                );

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
        Dialogs.showMessageDialog(
                this,
                ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Terjadi kesalahan",
                JOptionPane.ERROR_MESSAGE
        );
    }
}