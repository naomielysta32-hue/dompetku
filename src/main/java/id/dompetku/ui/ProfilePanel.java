package id.dompetku.ui;

import id.dompetku.dao.PenggunaDAO;
import id.dompetku.model.Pengguna;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Panel halaman profil: identitas akun, ubah nama, dan ganti password.
 */
// [OOP - Inheritance]: ProfilePanel mewarisi JPanel sebagai sub-halaman di dalam DashboardFrame.
// [OOP - Composition]: Panel ini memiliki objek Pengguna, PenggunaDAO, Theme.Avatar, dan callback onProfileChanged.
public class ProfilePanel extends JPanel {

    private final Pengguna user;
    private final PenggunaDAO dao = new PenggunaDAO();
    private final Runnable onProfileChanged;

    private final Theme.Avatar avatar;
    private final JLabel lblNama = Theme.label("", Font.BOLD, 18, Theme.TEXT);
    private final JLabel lblUsername = Theme.label("", Font.PLAIN, 13, Theme.HINT);
    private final JLabel lblSejak = Theme.label("", Font.PLAIN, 12, Theme.HINT);
    private final JLabel lblDompet = Theme.label("-", Font.BOLD, 22, Theme.TEXT);
    private final JLabel lblTransaksi = Theme.label("-", Font.BOLD, 22, Theme.TEXT);

    private final JTextField fieldNama = new JTextField();
    private final JTextField fieldUsername = new JTextField();
    private final JPasswordField fieldLama = new JPasswordField();
    private final JPasswordField fieldBaru = new JPasswordField();
    private final JPasswordField fieldUlang = new JPasswordField();

    public ProfilePanel(Pengguna user, Runnable onProfileChanged) {
        super(new BorderLayout(0, 12));
        this.user = user;
        this.onProfileChanged = onProfileChanged;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        avatar = new Theme.Avatar(user.getNama(), 92);
        add(Theme.pageHeader("Profil Akun", null), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        addCard(columns, identityCard(), 0, 0, 0, GridBagConstraints.BOTH, new Insets(0, 0, 16, 18));
        addCard(columns, accountCard(), 1, 0, 1, GridBagConstraints.BOTH, new Insets(0, 0, 16, 0));
        addCard(columns, passwordCard(), 1, 1, 1, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0; filler.gridy = 2; filler.gridwidth = 2; filler.weighty = 1;
        columns.add(Box.createGlue(), filler);

        add(Theme.scroll(columns), BorderLayout.CENTER);
        reload();
    }

    private static void addCard(JPanel parent, JPanel card, int gx, int gy, double wx, int fill, Insets insets) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = gx; gc.gridy = gy; gc.weightx = wx; gc.fill = fill;
        gc.anchor = GridBagConstraints.NORTH; gc.insets = insets;
        parent.add(card, gc);
    }

    public void reload() {
        avatar.setPerson(user.getNama());
        lblNama.setText(user.getNama());
        lblUsername.setText("@" + user.getUsername());
        fieldNama.setText(user.getNama());
        fieldUsername.setText(user.getUsername());
        lblSejak.setText(user.getTanggalDaftar() == null ? " "
                : "Bergabung sejak " + user.getTanggalDaftar().atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))));
        try {
            var st = dao.statistik(user.getIdPengguna());
            lblDompet.setText(String.valueOf(st.jumlahDompet()));
            lblTransaksi.setText(String.valueOf(st.jumlahTransaksi()));
        } catch (Exception ex) {
            lblDompet.setText("-");
            lblTransaksi.setText("-");
        }
    }

    private JPanel identityCard() {
        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(260, 320));
        card.setMinimumSize(new Dimension(260, 320));

        for (JComponent c : new JComponent[]{avatar, lblNama, lblUsername, lblSejak}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JPanel stats = new JPanel(new GridLayout(1, 2, 10, 0));
        stats.setOpaque(false);
        stats.add(stat(lblDompet, "Dompet aktif"));
        stats.add(stat(lblTransaksi, "Transaksi"));
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        card.add(Box.createVerticalGlue());
        card.add(avatar); card.add(Box.createVerticalStrut(14));
        card.add(lblNama); card.add(Box.createVerticalStrut(2));
        card.add(lblUsername); card.add(Box.createVerticalStrut(6));
        card.add(lblSejak); card.add(Box.createVerticalStrut(18));
        card.add(sep); card.add(Box.createVerticalStrut(16));
        card.add(stats); card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel stat(JLabel value, String caption) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel c = Theme.label(caption, Font.PLAIN, 11, Theme.HINT);
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(value); p.add(c);
        return p;
    }

    private JPanel accountCard() {
        JPanel card = formCard("Data Akun", "Nama ini tampil di menu samping.");
        JPanel form = (JPanel) card.getClientProperty("form");

        Theme.styleInput(fieldNama);
        Theme.styleInput(fieldUsername);
        Theme.filterNoDigits(fieldNama);
        fieldUsername.setEditable(false);
        fieldUsername.setFocusable(false);
        fieldUsername.setForeground(Theme.HINT);
        fieldUsername.setBackground(new Color(243, 244, 246));
        Theme.uniformSize(fieldNama);
        Theme.uniformSize(fieldUsername);

        addRow(form, 0, "Nama lengkap", fieldNama);
        addRow(form, 2, "Username (tidak dapat diubah)", fieldUsername);

        JButton save = Theme.button("Simpan Perubahan");
        save.addActionListener(e -> saveProfile());
        addButton(form, 4, save);
        return card;
    }

    private void saveProfile() {
        try {
            String namaBaru = fieldNama.getText().trim();
            if (namaBaru.isEmpty()) throw new IllegalArgumentException("Nama lengkap wajib diisi.");
            if (namaBaru.matches(".*\\d.*")) throw new IllegalArgumentException("Nama lengkap tidak boleh mengandung angka.");
            dao.ubahProfil(user.getIdPengguna(), namaBaru, user.getUsername());
            user.setNama(namaBaru);
            reload();
            onProfileChanged.run();
            Dialogs.showMessageDialog(this, "Profil berhasil diperbarui.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private JPanel passwordCard() {
        JPanel card = formCard("Ganti Password", "Gunakan minimal 8 karakter kombinasi huruf dan angka.");
        JPanel form = (JPanel) card.getClientProperty("form");
        addRow(form, 0, "Password lama", Theme.passwordWithEye(fieldLama, true));
        addRow(form, 2, "Password baru", Theme.passwordWithEye(fieldBaru, true));
        addRow(form, 4, "Ulangi password baru", Theme.passwordWithEye(fieldUlang, true));

        JButton save = Theme.buttonDark("Ubah Password");
        save.addActionListener(e -> savePassword());
        addButton(form, 6, save);
        return card;
    }

    private void savePassword() {
        try {
            String lama = new String(fieldLama.getPassword());
            String baru = new String(fieldBaru.getPassword());
            String ulang = new String(fieldUlang.getPassword());
            if (lama.isEmpty() || baru.isEmpty() || ulang.isEmpty()) throw new IllegalArgumentException("Semua kolom password wajib diisi.");
            if (!baru.equals(ulang)) throw new IllegalArgumentException("Password baru dan ulangannya tidak sama.");
            if (!baru.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                throw new IllegalArgumentException("Password baru harus mengandung kombinasi huruf dan angka.");
            }
            dao.ubahPassword(user.getIdPengguna(), lama, baru);
            fieldLama.setText(""); fieldBaru.setText(""); fieldUlang.setText("");
            Dialogs.showMessageDialog(this, "Password berhasil diubah.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private JPanel formCard(String title, String subtitle) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Theme.label(title, 16, true));
        head.add(Box.createVerticalStrut(2));
        head.add(Theme.label(subtitle, Font.PLAIN, 12, Theme.HINT));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        card.add(head, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.putClientProperty("form", form);
        return card;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST;
        c.gridy = row; c.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);
        form.add(Theme.label(label, 12, true), c);
        c.gridy = row + 1; c.insets = new Insets(0, 0, 0, 0);
        form.add(field, c);
    }

    private void addButton(JPanel form, int row, JButton button) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(16, 0, 0, 0);
        form.add(button, c);
    }

    private void showError(Exception ex) {
        Dialogs.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Tidak bisa disimpan", JOptionPane.WARNING_MESSAGE);
    }
}
