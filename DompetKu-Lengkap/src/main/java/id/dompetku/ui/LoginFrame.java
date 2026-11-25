package id.dompetku.ui;

import id.dompetku.dao.PenggunaDAO;
import id.dompetku.model.Pengguna;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
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

        // Revisi #14: ikon mata pada kolom password login
        JPanel passwordWrapper = buatPasswordDenganMata(password);

        gbc.gridy = 0;
        form.add(usernameLabel, gbc);

        gbc.gridy = 1;
        form.add(username, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 5, 0);
        form.add(passwordLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 5, 0);
        form.add(passwordWrapper, gbc);

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

    /** Revisi #14: membungkus JPasswordField dengan tombol ikon mata (default tersembunyi). */
    private JPanel buatPasswordDenganMata(JPasswordField pf) {
        char defaultEcho = pf.getEchoChar() == 0 ? '•' : pf.getEchoChar();
        pf.setEchoChar(defaultEcho);

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(pf.getBackground());
        wrapper.setBorder(pf.getBorder());

        pf.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        pf.setOpaque(false);

        JToggleButton btnMata = new JToggleButton("👁");
        btnMata.setToolTipText("Tampilkan / Sembunyikan Password");
        btnMata.setFocusPainted(false);
        btnMata.setBorderPainted(false);
        btnMata.setContentAreaFilled(false);
        btnMata.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMata.setForeground(Theme.HINT);
        btnMata.setMargin(new Insets(0, 4, 0, 0));
        btnMata.addActionListener(e -> {
            if (btnMata.isSelected()) {
                pf.setEchoChar((char) 0);
                btnMata.setForeground(Theme.CHARCOAL);
            } else {
                pf.setEchoChar(defaultEcho);
                btnMata.setForeground(Theme.HINT);
            }
        });

        wrapper.add(pf, BorderLayout.CENTER);
        wrapper.add(btnMata, BorderLayout.EAST);
        return wrapper;
    }

    /** Revisi #9: memblokir input karakter angka pada field nama. */
    private void pasangFilterTanpaAngka(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null) {
                    string = string.replaceAll("\\d", "");
                }
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null) {
                    text = text.replaceAll("\\d", "");
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
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

        // Revisi #9: blokir angka di nama lengkap saat registrasi
        pasangFilterTanpaAngka(nama);

        // Revisi #14: ikon mata di password registrasi
        JPanel passWrapper = buatPasswordDenganMata(pass);

        Object[] fields = {
                "Nama lengkap", nama,
                "Username (minimal 3 karakter)", user,
                "Password (min. 8 karakter, huruf & angka)", passWrapper
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
            String namaText = nama.getText().trim();
            String passText = new String(pass.getPassword());

            // Revisi #9: validasi nama tanpa angka
            if (namaText.matches(".*\\d.*")) {
                throw new IllegalArgumentException("Nama lengkap tidak boleh mengandung angka.");
            }

            // Revisi #16: validasi password kombinasi huruf dan angka
            if (!passText.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                throw new IllegalArgumentException("Password harus mengandung kombinasi huruf dan angka.");
            }

            Pengguna p = new PenggunaDAO().daftar(
                    namaText,
                    user.getText(),
                    passText
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

        // Revisi #9: blokir angka pada input nama lengkap
        pasangFilterTanpaAngka(nama);

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

        // Revisi #14: ikon mata di form password baru
        JPanel baruWrapper = buatPasswordDenganMata(baru);
        JPanel ulangiWrapper = buatPasswordDenganMata(ulangi);

        Object[] fieldsBaru = {
                "Password baru (min. 8 karakter, huruf & angka)", baruWrapper,
                "Ulangi password baru", ulangiWrapper
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

            // Revisi #16: password baru harus mengandung kombinasi huruf dan angka
            if (!p1.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                Dialogs.showMessageDialog(
                        this,
                        "Password harus mengandung kombinasi huruf dan angka.",
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