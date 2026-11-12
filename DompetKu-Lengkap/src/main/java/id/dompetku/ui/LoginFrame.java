package id.dompetku.ui;

import id.dompetku.dao.PenggunaDAO;
import id.dompetku.model.Pengguna;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        super("DompetKu • Masuk");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 440);
        setMinimumSize(new Dimension(420, 400));
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

        JButton masuk = Theme.button("Masuk");
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
        try {
            String uText = username.getText().trim();
            String pText = new String(password.getPassword());

            if (uText.isEmpty() || pText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Username dan password tidak boleh kosong.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            Optional<Pengguna> userOpt = new PenggunaDAO().login(uText, pText);

            if (userOpt.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Username atau password salah.",
                        "Gagal Masuk",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            new DashboardFrame(userOpt.get()).setVisible(true);
            dispose();

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

        if (JOptionPane.showConfirmDialog(
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

            JOptionPane.showMessageDialog(
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

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Terjadi kesalahan",
                JOptionPane.ERROR_MESSAGE
        );
    }
}