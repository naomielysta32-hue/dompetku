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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Halaman profil: identitas akun, ubah nama (username terkunci), dan ganti password.
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

        // Baris 0 Kolom 0: Kartu Identitas (tingginya otomatis sejajar dengan kartu Data Akun di sebelah kanan)
        GridBagConstraints gcLeft = new GridBagConstraints();
        gcLeft.gridx = 0;
        gcLeft.gridy = 0;
        gcLeft.weightx = 0;
        gcLeft.weighty = 0;
        gcLeft.fill = GridBagConstraints.BOTH;
        gcLeft.anchor = GridBagConstraints.NORTH;
        gcLeft.insets = new Insets(0, 0, 16, 18);
        columns.add(identityCard(), gcLeft);

        // Baris 0 Kolom 1: Kartu Data Akun (sejajar atas & bawah dengan Kartu Identitas)
        GridBagConstraints gcRightTop = new GridBagConstraints();
        gcRightTop.gridx = 1;
        gcRightTop.gridy = 0;
        gcRightTop.weightx = 1;
        gcRightTop.weighty = 0;
        gcRightTop.fill = GridBagConstraints.BOTH;
        gcRightTop.anchor = GridBagConstraints.NORTH;
        gcRightTop.insets = new Insets(0, 0, 16, 0);
        columns.add(accountCard(), gcRightTop);

        // Baris 1 Kolom 1: Kartu Ganti Password
        GridBagConstraints gcRightBottom = new GridBagConstraints();
        gcRightBottom.gridx = 1;
        gcRightBottom.gridy = 1;
        gcRightBottom.weightx = 1;
        gcRightBottom.weighty = 0;
        gcRightBottom.fill = GridBagConstraints.HORIZONTAL;
        gcRightBottom.anchor = GridBagConstraints.NORTH;
        gcRightBottom.insets = new Insets(0, 0, 0, 0);
        columns.add(passwordCard(), gcRightBottom);

        // Dorong seluruh isi ke atas bila jendela tinggi
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 2;
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
        card.setPreferredSize(new Dimension(260, 320));
        card.setMinimumSize(new Dimension(260, 320));

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
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        card.add(Box.createVerticalGlue());
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
        card.add(Box.createVerticalGlue());
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

        // Revisi #9: blokir input angka pada nama lengkap
        pasangFilterTanpaAngka(fieldNama);

        // Revisi #15: username tidak bisa diubah
        fieldUsername.setEditable(false);
        fieldUsername.setFocusable(false);
        fieldUsername.setForeground(Theme.HINT);
        fieldUsername.setBackground(new Color(243, 244, 246));

        // Revisi #13: samakan tinggi semua kotak input di profil
        samakanUkuranKotak(fieldNama);
        samakanUkuranKotak(fieldUsername);

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
            if (namaBaru.isEmpty()) {
                throw new IllegalArgumentException("Nama lengkap wajib diisi.");
            }
            if (namaBaru.matches(".*\\d.*")) {
                throw new IllegalArgumentException("Nama lengkap tidak boleh mengandung angka.");
            }

            // Username tetap memakai user.getUsername() agar tidak berubah
            dao.ubahProfil(user.getIdPengguna(), namaBaru, user.getUsername());
            user.setNama(namaBaru);
            reload();
            onProfileChanged.run();
            Dialogs.showMessageDialog(this, "Profil berhasil diperbarui.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------ form password

    private JPanel passwordCard() {
        JPanel card = formCard("Ganti Password", "Gunakan minimal 8 karakter kombinasi huruf dan angka.");
        JPanel form = (JPanel) card.getClientProperty("form");

        Theme.styleInput(fieldLama);
        Theme.styleInput(fieldBaru);
        Theme.styleInput(fieldUlang);

        // Revisi #13 & #14: bungkus password dengan tombol mata dan samakan ukuran kotaknya
        JPanel wrapLama = buatPasswordDenganMata(fieldLama);
        JPanel wrapBaru = buatPasswordDenganMata(fieldBaru);
        JPanel wrapUlang = buatPasswordDenganMata(fieldUlang);

        addRow(form, 0, "Password lama", wrapLama);
        addRow(form, 2, "Password baru", wrapBaru);
        addRow(form, 4, "Ulangi password baru", wrapUlang);

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
            // Revisi #16: wajib ada kombinasi huruf dan angka
            if (!baru.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                throw new IllegalArgumentException("Password baru harus mengandung kombinasi huruf dan angka.");
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

    // ------------------------------------------------------------------ helper revisi & tata letak

    /** Revisi #13: menyeragamkan ukuran tinggi semua kotak input agar konsisten. */
    private void samakanUkuranKotak(JComponent comp) {
        Dimension dim = new Dimension(360, 38);
        comp.setPreferredSize(dim);
        comp.setMinimumSize(new Dimension(120, 38));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    /** Revisi #14: membuat wrapper input password dengan tombol ikon mata di sisi kanan. */
    private JPanel buatPasswordDenganMata(JPasswordField pf) {
        char defaultEcho = pf.getEchoChar() == 0 ? '•' : pf.getEchoChar();
        pf.setEchoChar(defaultEcho);

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(pf.getBackground());
        wrapper.setBorder(pf.getBorder());
        samakanUkuranKotak(wrapper);

        pf.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));
        pf.setOpaque(false);

        JToggleButton btnMata = new JToggleButton("👁");
        btnMata.setToolTipText("Tampilkan / Sembunyikan Password");
        btnMata.setFocusPainted(false);
        btnMata.setBorderPainted(false);
        btnMata.setContentAreaFilled(false);
        btnMata.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMata.setForeground(Theme.HINT);
        btnMata.setPreferredSize(new Dimension(38, 36));
        btnMata.addActionListener(e -> {
            if (btnMata.isSelected()) {
                pf.setEchoChar((char) 0);
                btnMata.setForeground(Theme.TEXT);
            } else {
                pf.setEchoChar(defaultEcho);
                btnMata.setForeground(Theme.HINT);
            }
        });

        wrapper.add(pf, BorderLayout.CENTER);
        wrapper.add(btnMata, BorderLayout.EAST);
        return wrapper;
    }

    /** Revisi #9: memblokir karakter angka agar tidak dapat diketik atau di-paste ke field nama. */
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