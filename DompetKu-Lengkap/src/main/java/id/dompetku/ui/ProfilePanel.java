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
 * Halaman profil: identitas akun, ubah nama/username, dan ganti password.
 * Semua form tampil langsung di halaman (tanpa dialog) agar mudah dipakai.
 */
public class ProfilePanel extends JPanel {

    private final Pengguna user;
    private final PenggunaDAO dao = new PenggunaDAO();
    private final Runnable onProfileChanged;

    private final Theme.Avatar avatar;
    private final JLabel lblNama = new JLabel();
    private final JLabel lblUsername = new JLabel();
    private final JLabel lblSejak = new JLabel();
    private final JLabel lblDompet = new JLabel("-");
    private final JLabel lblTransaksi = new JLabel("-");

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

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTH;

        gc.gridx = 0;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.insets = new Insets(0, 0, 0, 18);
        gc.fill = GridBagConstraints.HORIZONTAL; // kartu identitas tidak ikut memanjang ke bawah
        columns.add(identityCard(), gc);
        gc.fill = GridBagConstraints.BOTH;

        JPanel forms = new JPanel();
        forms.setOpaque(false);
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));
        forms.add(accountCard());
        forms.add(Box.createVerticalStrut(16));
        forms.add(passwordCard());

        gc.gridx = 1;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        columns.add(forms, gc);

        // dorong isi ke atas bila jendela tinggi
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 1;
        filler.gridwidth = 2;
        filler.weighty = 1;
        columns.add(Box.createGlue(), filler);

        JScrollPane scroll = new JScrollPane(columns);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        reload();
    }

    /** Muat ulang angka & identitas (dipanggil saat halaman dibuka). */
    public void reload() {
        avatar.setPerson(user.getNama());
        lblNama.setText(user.getNama());
        lblUsername.setText("@" + user.getUsername());
        fieldNama.setText(user.getNama());
        fieldUsername.setText(user.getUsername());

        if (user.getTanggalDaftar() != null) {
            lblSejak.setText("Bergabung sejak " + user.getTanggalDaftar()
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))));
        } else {
            lblSejak.setText(" ");
        }

        try {
            var st = dao.statistik(user.getIdPengguna());
            lblDompet.setText(String.valueOf(st.jumlahDompet()));
            lblTransaksi.setText(String.valueOf(st.jumlahTransaksi()));
        } catch (Exception ex) {
            lblDompet.setText("-");
            lblTransaksi.setText("-");
        }
    }

    // ------------------------------------------------------------------ kartu identitas

    private JPanel identityCard() {
        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(260, 300));

        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblNama.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblNama.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblUsername.setForeground(Theme.HINT);
        lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSejak.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSejak.setForeground(Theme.HINT);
        lblSejak.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel stats = new JPanel(new GridLayout(1, 2, 10, 0));
        stats.setOpaque(false);
        stats.add(stat(lblDompet, "Dompet aktif"));
        stats.add(stat(lblTransaksi, "Transaksi"));
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        card.add(Box.createVerticalStrut(6));
        card.add(avatar);
        card.add(Box.createVerticalStrut(14));
        card.add(lblNama);
        card.add(Box.createVerticalStrut(2));
        card.add(lblUsername);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSejak);
        card.add(Box.createVerticalStrut(18));
        card.add(sep);
        card.add(Box.createVerticalStrut(16));
        card.add(stats);
        return card;
    }

    private JPanel stat(JLabel value, String caption) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        value.setFont(new Font("SansSerif", Font.BOLD, 22));
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel c = new JLabel(caption);
        c.setFont(new Font("SansSerif", Font.PLAIN, 11));
        c.setForeground(Theme.HINT);
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(value);
        p.add(c);
        return p;
    }

    // ------------------------------------------------------------------ form data akun

    private JPanel accountCard() {
        JPanel card = formCard("Data Akun", "Nama ini tampil di menu samping.");
        JPanel form = (JPanel) card.getClientProperty("form");

        Theme.styleInput(fieldNama);
        Theme.styleInput(fieldUsername);
        addRow(form, 0, "Nama lengkap", fieldNama);
        addRow(form, 2, "Username (minimal 3 karakter)", fieldUsername);

        JButton save = Theme.button("Simpan Perubahan");
        save.addActionListener(e -> saveProfile());
        addButton(form, 4, save);
        return card;
    }

    private void saveProfile() {
        try {
            dao.ubahProfil(user.getIdPengguna(), fieldNama.getText(), fieldUsername.getText());
            user.setNama(fieldNama.getText().trim());
            user.setUsername(fieldUsername.getText().trim());
            reload();
            onProfileChanged.run();
            Dialogs.showMessageDialog(this, "Profil berhasil diperbarui.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------ form password

    private JPanel passwordCard() {
        JPanel card = formCard("Ganti Password", "Gunakan minimal 8 karakter.");
        JPanel form = (JPanel) card.getClientProperty("form");

        Theme.styleInput(fieldLama);
        Theme.styleInput(fieldBaru);
        Theme.styleInput(fieldUlang);
        addRow(form, 0, "Password lama", fieldLama);
        addRow(form, 2, "Password baru", fieldBaru);
        addRow(form, 4, "Ulangi password baru", fieldUlang);

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

            if (lama.isEmpty() || baru.isEmpty() || ulang.isEmpty()) {
                throw new IllegalArgumentException("Semua kolom password wajib diisi.");
            }
            if (!baru.equals(ulang)) {
                throw new IllegalArgumentException("Password baru dan ulangannya tidak sama.");
            }

            dao.ubahPassword(user.getIdPengguna(), lama, baru);
            fieldLama.setText("");
            fieldBaru.setText("");
            fieldUlang.setText("");
            Dialogs.showMessageDialog(this, "Password berhasil diubah.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------ helper tata letak

    /** Kartu dengan judul + keterangan; isi form ada di client property "form". */
    private JPanel formCard(String title, String subtitle) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(Theme.HINT);
        head.add(t);
        head.add(Box.createVerticalStrut(2));
        head.add(s);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        card.add(head, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.putClientProperty("form", form);
        return card;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        c.gridy = row;
        c.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);
        form.add(l, c);

        c.gridy = row + 1;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(field, c);
    }

    private void addButton(JPanel form, int row, JButton button) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(16, 0, 0, 0);
        form.add(button, c);
    }

    private void showError(Exception ex) {
        String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        Dialogs.showMessageDialog(this, msg, "Tidak bisa disimpan", JOptionPane.WARNING_MESSAGE);
    }
}
