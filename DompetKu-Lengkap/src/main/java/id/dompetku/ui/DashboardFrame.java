package id.dompetku.ui;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.toedter.calendar.JDateChooser;
import id.dompetku.dao.KeuanganDAO;
import id.dompetku.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * Jendela utama aplikasi setelah pengguna masuk.
 * Mengelola navigasi CardLayout antar modul keuangan dan berinteraksi dengan KeuanganDAO.
 */
// [OOP - Inheritance]: DashboardFrame mewarisi JFrame sebagai jendela utama aplikasi desktop.
// [OOP - Composition]: DashboardFrame tersusun dari Pengguna, KeuanganDAO, Sidebar, ProfilePanel, dan panel halaman.
public class DashboardFrame extends JFrame {

    private final Pengguna user;
    private final KeuanganDAO dao = new KeuanganDAO();
    private final NumberFormat rupiah = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    { rupiah.setMinimumFractionDigits(0); rupiah.setMaximumFractionDigits(0); }

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter bulanFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"));
    private final List<Runnable> refreshTasks = new ArrayList<>();

    private final JLabel lblTotalSaldo = new JLabel("Rp 0", SwingConstants.LEFT);
    private final JLabel lblPemasukan = new JLabel("Rp 0", SwingConstants.LEFT);
    private final JLabel lblPengeluaran = new JLabel("Rp 0", SwingConstants.LEFT);
    private final JLabel lblSelisih = new JLabel("Rp 0", SwingConstants.LEFT);
    private final JLabel lblStatusTarget = new JLabel("Memuat...", SwingConstants.LEFT);
    private final JLabel lblStatusUtang = new JLabel("Memuat...", SwingConstants.LEFT);

    private final JTable tblRincian = new JTable();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final ProfilePanel profilePanel;

    public DashboardFrame(Pengguna u) {
        super("DompetKu • Dashboard Keuangan");
        this.user = u;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        sidebar = new Sidebar(u.getNama(), u.getUsername(), this::navigate, this::logout);
        sidebar.section("MENU UTAMA");
        sidebar.item("ringkasan", "Ringkasan", "home");
        sidebar.item("dompet", "Dompet", "wallet");
        sidebar.item("transaksi", "Transaksi", "list");
        sidebar.section("PERENCANAAN");
        sidebar.item("anggaran", "Anggaran", "pie");
        sidebar.item("tabungan", "Target Tabungan", "target");
        sidebar.item("utang", "Utang-Piutang", "swap");
        sidebar.section("LAINNYA");
        sidebar.item("laporan", "Laporan & Ekspor", "chart");

        profilePanel = new ProfilePanel(user, () -> sidebar.setUser(user.getNama(), user.getUsername()));
        content.setBackground(Theme.BG);
        content.add(summary(), "ringkasan");
        content.add(wallets(), "dompet");
        content.add(transactions(), "transaksi");
        content.add(budgets(), "anggaran");
        content.add(savings(), "tabungan");
        content.add(debts(), "utang");
        content.add(reports(), "laporan");
        content.add(profilePanel, Sidebar.PROFILE_KEY);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.BG);
        shell.add(sidebar, BorderLayout.WEST);
        shell.add(content, BorderLayout.CENTER);
        setContentPane(shell);
        navigate("ringkasan");
    }

    private void navigate(String page) {
        if (Sidebar.PROFILE_KEY.equals(page)) profilePanel.reload();
        cards.show(content, page);
        sidebar.select(page);
    }

    private void logout() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    // ================= RINGKASAN =================

    private JPanel summary() {
        JPanel p = Theme.page();
        p.setLayout(new BorderLayout(0, 14));

        JPanel headerAndNotif = new JPanel(new BorderLayout(0, 8));
        headerAndNotif.setOpaque(false);
        headerAndNotif.add(Theme.heading("Ringkasan Keuangan Bulanan"), BorderLayout.NORTH);
        headerAndNotif.add(createNotificationPanel(), BorderLayout.CENTER);

        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.add(createStatCard("Total Saldo", lblTotalSaldo, Theme.TEXT, Theme.WHITE));
        cardsGrid.add(createStatCard("Pemasukan Bulan Ini", lblPemasukan, new Color(15, 81, 50), new Color(212, 237, 218)));
        cardsGrid.add(createStatCard("Pengeluaran Bulan Ini", lblPengeluaran, new Color(114, 28, 36), new Color(248, 215, 218)));
        cardsGrid.add(createStatCard("Selisih / Cashflow", lblSelisih, Theme.GOLD, Theme.WHITE));

        JPanel statusWidgetsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        statusWidgetsGrid.setOpaque(false);
        statusWidgetsGrid.setBorder(new EmptyBorder(8, 0, 0, 0));
        statusWidgetsGrid.add(createClickableStatCard("Status Target Tabungan", lblStatusTarget, Theme.TEXT, Theme.WHITE,
                "Klik untuk membuka halaman Target Tabungan", () -> navigate("tabungan")));
        statusWidgetsGrid.add(createClickableStatCard("Status Catatan Utang", lblStatusUtang, Theme.TEXT, Theme.WHITE,
                "Klik untuk membuka halaman Utang-Piutang", () -> navigate("utang")));

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setOpaque(false);
        statsWrapper.add(cardsGrid, BorderLayout.NORTH);
        statsWrapper.add(statusWidgetsGrid, BorderLayout.CENTER);

        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);
        topContainer.add(headerAndNotif, BorderLayout.NORTH);
        topContainer.add(statsWrapper, BorderLayout.CENTER);
        p.add(topContainer, BorderLayout.NORTH);

        JPanel walletDetailPanel = new JPanel(new BorderLayout(0, 6));
        walletDetailPanel.setOpaque(false);
        walletDetailPanel.add(Theme.label("Rincian Dompet & Akun", 15, true), BorderLayout.NORTH);
        Theme.styleTable(tblRincian);
        walletDetailPanel.add(Theme.tableScroll(tblRincian), BorderLayout.CENTER);

        JPanel tipPanel = Theme.borderedCard(Theme.WHITE, Theme.BORDER, 10, 14);
        tipPanel.setLayout(new BorderLayout(12, 0));
        tipPanel.add(Theme.label("💡", 16, false), BorderLayout.WEST);
        tipPanel.add(Theme.label("<html><b>Tips Keuangan Mahasiswa:</b> Biasakan mencatat setiap pengeluaran sekecil apapun dan sisihkan minimal 10% uang saku bulanan di awal untuk pos tabungan darurat.</html>",
                12, false), BorderLayout.CENTER);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(walletDetailPanel, BorderLayout.CENTER);
        centerContainer.add(tipPanel, BorderLayout.SOUTH);
        p.add(centerContainer, BorderLayout.CENTER);

        loadSummary();
        return p;
    }

    private JPanel createNotificationPanel() {
        JPanel notifPanel = new JPanel();
        notifPanel.setLayout(new BoxLayout(notifPanel, BoxLayout.Y_AXIS));
        notifPanel.setOpaque(false);
        try {
            LocalDate today = LocalDate.now();
            for (String[] u : dao.utang(user.getIdPengguna())) {
                if (!"LUNAS".equalsIgnoreCase(u[6]) && u[5] != null) {
                    long days = ChronoUnit.DAYS.between(today, LocalDate.parse(u[5]));
                    String pihak = ("PIUTANG".equalsIgnoreCase(u[1]) ? "Piutang dari " : "Utang kepada ") + u[2];
                    if (days < 0) notifPanel.add(createAlertBox("⚠️ PERINGATAN: " + pihak + " telah LEWAT JATUH TEMPO (" + u[5] + ")!", Theme.EXPENSE));
                    else if (days == 0) notifPanel.add(createAlertBox("🔔 PERINGATAN: " + pihak + " jatuh tempo HARI INI (" + u[5] + ").", Theme.GOLD));
                    else if (days <= 3) notifPanel.add(createAlertBox("🔔 PERINGATAN: " + pihak + " akan jatuh tempo dalam " + days + " hari (" + u[5] + ").", Theme.GOLD));
                }
            }
            for (String[] a : dao.anggaran(user.getIdPengguna())) {
                BigDecimal limit = new BigDecimal(a[4]), used = new BigDecimal(a[5]);
                if (limit.signum() > 0) {
                    BigDecimal pct = used.multiply(new BigDecimal("100")).divide(limit, 0, RoundingMode.HALF_UP);
                    if (used.compareTo(limit) > 0) {
                        notifPanel.add(createAlertBox("🚨 ANGGARAN TERLAMPAUI: Kategori '" + a[1] + "' melebihi batas sebesar "
                                + money(used.subtract(limit)) + " (" + pct + "% dari batas anggaran)!", Theme.EXPENSE));
                    } else if (pct.compareTo(new BigDecimal("100")) >= 0) {
                        notifPanel.add(createAlertBox("🚨 ANGGARAN HABIS: Kategori '" + a[1] + "' telah mencapai " + pct + "% dari batas anggaran!", Theme.EXPENSE));
                    } else if (pct.compareTo(new BigDecimal("80")) >= 0) {
                        notifPanel.add(createAlertBox("⚠️ PERINGATAN ANGGARAN: Kategori '" + a[1] + "' sudah terpakai " + pct + "%!", Theme.GOLD));
                    }
                }
            }
        } catch (Exception ignored) {}
        return notifPanel;
    }

    private JPanel createAlertBox(String message, Color color) {
        boolean warning = color == Theme.GOLD;
        JPanel box = Theme.borderedCard(warning ? new Color(0xFF, 0xF4, 0xD6) : new Color(255, 240, 240), color, 8, 12);
        box.setLayout(new BorderLayout());
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        box.add(Theme.label(message, Font.BOLD, 12, warning ? new Color(0x7A, 0x56, 0x00) : color), BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 6, 0));
        wrapper.add(box, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor, Color backgroundColor) {
        JPanel card = Theme.borderedCard(backgroundColor, Theme.BORDER, 12, 14);
        card.setLayout(new BorderLayout(0, 6));
        valueLabel.setFont(Theme.font(Font.BOLD, 16));
        valueLabel.setForeground(accentColor);
        card.add(Theme.label(title.toUpperCase(), Font.BOLD, 10, Theme.TEXT), BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createClickableStatCard(String title, JLabel valueLabel, Color accentColor,
                                           Color backgroundColor, String tooltip, Runnable onClick) {
        JPanel card = createStatCard(title, valueLabel, accentColor, backgroundColor);
        Color hoverColor = new Color(250, 246, 235);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(tooltip);
        Theme.onClick(card, onClick);
        Theme.onHover(card, h -> card.setBackground(h ? hoverColor : backgroundColor));
        return card;
    }

    private void loadSummary() {
        try {
            DefaultTableModel rincian = model("Nama Dompet", "Jenis", "Saldo");
            BigDecimal total = BigDecimal.ZERO;
            for (var w : dao.dompet(user.getIdPengguna())) {
                rincian.addRow(new Object[]{w.nama(), w.jenis(), money(w.saldo())});
                total = total.add(w.saldo());
            }
            tblRincian.setModel(rincian);

            var r = dao.ringkasan(user.getIdPengguna(), YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth());
            lblTotalSaldo.setText(money(total));
            lblPemasukan.setText(money(r.pemasukan()));
            lblPengeluaran.setText(money(r.pengeluaran()));
            lblSelisih.setText(money(r.selisih()));

            var targets = dao.target(user.getIdPengguna());
            long activeTargets = targets.stream().filter(t -> !"TERCAPAI".equalsIgnoreCase(t[4])).count();
            lblStatusTarget.setText(targets.isEmpty() ? "Belum ada target" : activeTargets + " Target Aktif Berjalan");

            var debts = dao.utang(user.getIdPengguna());
            var unpaid = debts.stream().filter(d -> !"LUNAS".equalsIgnoreCase(d[6])).toList();
            if (debts.isEmpty()) lblStatusUtang.setText("Tidak ada catatan");
            else if (unpaid.isEmpty()) lblStatusUtang.setText("Semua catatan sudah lunas");
            else {
                final int maxShown = 3;
                StringBuilder sb = new StringBuilder("<html>").append(unpaid.size())
                        .append(" Catatan Belum Lunas<div style='font-size:11px;font-weight:normal'>");
                for (int i = 0; i < Math.min(maxShown, unpaid.size()); i++) {
                    String[] d = unpaid.get(i);
                    BigDecimal sisa = new BigDecimal(d[3]).subtract(new BigDecimal(d[4]));
                    sb.append("• ").append(escape(d[1])).append(" - ").append(escape(d[2]))
                            .append(" (sisa ").append(money(sisa)).append(")<br>");
                }
                if (unpaid.size() > maxShown) sb.append("+ ").append(unpaid.size() - maxShown).append(" lainnya...");
                lblStatusUtang.setText(sb.append("</div></html>").toString());
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    // ================= DOMPET =================

    private JPanel wallets() {
        JPanel p = Theme.page();
        JTable table = new JTable();
        Theme.styleTable(table);

        JButton add = Theme.button("+ Tambah Dompet"), transfer = Theme.buttonDark("Transfer"),
                edit = Theme.buttonGhost("Edit"), delete = Theme.buttonDanger("Hapus");
        edit.setEnabled(false);
        delete.setEnabled(false);

        add.addActionListener(e -> addWallet());
        transfer.addActionListener(e -> transfer());
        edit.addActionListener(e -> editWallet(table));
        delete.addActionListener(e -> deleteWallet(table));
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = table.getSelectedRow() >= 0;
            edit.setEnabled(sel); delete.setEnabled(sel);
        });
        Theme.onDoubleClick(table, () -> editWallet(table));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 4, 0));
        toolbar.add(Theme.label("Pilih satu dompet di tabel untuk mengedit atau menghapusnya.", Font.PLAIN, 12, Theme.HINT), BorderLayout.WEST);
        toolbar.add(Theme.buttonRow(edit, delete, transfer, add), BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.tableScroll(table), BorderLayout.CENTER);

        p.add(Theme.pageHeader("Daftar Dompet & Rekening", null), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);

        loadWallets(table);
        refreshTasks.add(() -> loadWallets(table));
        return p;
    }

    private void loadWallets(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Nama Dompet", "Jenis", "Saldo");
            for (var r : dao.dompet(user.getIdPengguna())) m.addRow(new Object[]{r.id(), r.nama(), r.jenis(), money(r.saldo())});
            table.setModel(m);
            hideIdColumn(table);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addWallet() {
        JTextField nama = new JTextField();
        Theme.filterLettersOnly(nama);
        JComboBox<String> jenis = new JComboBox<>(new String[]{"TUNAI", "BANK", "EWALLET"});
        JTextField saldo = Theme.rupiahField("0");

        if (Dialogs.showConfirmDialog(this, new Object[]{"Nama Dompet (hanya huruf):", nama,
                "Jenis Akun:", jenis, "Saldo Awal (Rp):", saldo},
                "Tambah Dompet Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            KeuanganDAO.validasiNamaDompet(nama.getText());
            String namaFinal = nama.getText().trim();
            BigDecimal s = amountOrZero(saldo.getText());
            // [OOP - Polymorphism]: Referensi bertipe induk Dompet menampung objek RekeningBank, EWallet, atau DompetTunai.
            Dompet d = switch ((String) jenis.getSelectedItem()) {
                case "BANK" -> new RekeningBank(user.getIdPengguna(), namaFinal, s);
                case "EWALLET" -> new EWallet(user.getIdPengguna(), namaFinal, s);
                default -> new DompetTunai(user.getIdPengguna(), namaFinal, s);
            };
            dao.tambahDompet(d);
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void editWallet(JTable table) {
        Long id = selectedId(table);
        if (id == null) { info("Pilih dompet yang ingin diedit terlebih dahulu."); return; }
        try {
            var w = findWalletById(id);
            JTextField nama = new JTextField();
            Theme.filterLettersOnly(nama);
            nama.setText(w.nama());
            JComboBox<String> jenis = new JComboBox<>(new String[]{"TUNAI", "BANK", "EWALLET"});
            jenis.setSelectedItem(w.jenis());
            JTextField saldoAwal = Theme.rupiahField(plain(w.saldoAwal()));
            JLabel note = new JLabel("<html><div style='width:260px;color:#7A6A55'>Saldo saat ini <b>"
                    + money(w.saldo()) + "</b>. Mengubah saldo awal akan ikut mengubah saldo saat ini.</div></html>");

            if (Dialogs.showConfirmDialog(this, new Object[]{"Nama Dompet (hanya huruf):", nama,
                    "Jenis Akun:", jenis, "Saldo Awal (Rp):", saldoAwal, note},
                    "Edit Dompet", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            KeuanganDAO.validasiNamaDompet(nama.getText());
            dao.ubahDompet(user.getIdPengguna(), id, nama.getText().trim(), (String) jenis.getSelectedItem(), amountOrZero(saldoAwal.getText()));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void deleteWallet(JTable table) {
        Long id = selectedId(table);
        if (id == null) { info("Pilih dompet yang ingin dihapus terlebih dahulu."); return; }
        try {
            var w = findWalletById(id);
            int riwayat = dao.jumlahRiwayatDompet(user.getIdPengguna(), id);
            StringBuilder msg = new StringBuilder("<html><div style='width:300px'>");
            if (riwayat == 0) {
                msg.append("Hapus dompet <b>").append(escape(w.nama())).append("</b> secara permanen?");
            } else {
                msg.append("Dompet <b>").append(escape(w.nama())).append("</b> punya ").append(riwayat)
                        .append(" catatan transaksi/transfer.<br><br>Dompet akan disembunyikan dari daftar. Riwayat transaksinya tetap tersimpan.");
            }
            if (w.saldo().signum() != 0) {
                msg.append("<br><br>Saldo <b>").append(money(w.saldo())).append("</b> tidak akan ikut dihitung lagi di Total Saldo.");
            }
            Object[] options = {"Hapus", "Batal"};
            int choice = Dialogs.showOptionDialog(this, new JLabel(msg.append("</div></html>").toString()),
                    "Hapus Dompet", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (choice != 0) return;
            dao.hapusDompet(user.getIdPengguna(), id);
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private KeuanganDAO.DompetRow findWalletById(long id) throws SQLException {
        return dao.dompet(user.getIdPengguna()).stream().filter(x -> x.id() == id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Dompet tidak ditemukan. Muat ulang halaman."));
    }

    // ================= TRANSAKSI & TRANSFER =================

    private JPanel transactions() {
        JPanel p = Theme.page();
        JTable table = new JTable();
        Theme.styleTable(table);

        JTextField search = new JTextField(12);
        JComboBox<String> jenisFilter = new JComboBox<>(new String[]{"Semua Jenis", "Pemasukan", "Pengeluaran"});
        JButton find = Theme.button("Cari"), add = Theme.button("+ Catat Transaksi"), cat = Theme.buttonDark("+ Kelola Kategori");

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Pencarian:"));
        searchBar.add(search); searchBar.add(jenisFilter); searchBar.add(find);

        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        actionBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        actionBar.add(searchBar, BorderLayout.WEST);
        actionBar.add(Theme.buttonRow(cat, add), BorderLayout.EAST);

        final boolean[] resetting = {false};
        Runnable cari = () -> loadTransactions(table, search.getText(), jenisFilterKeDb(jenisFilter));
        find.addActionListener(e -> cari.run());
        search.addActionListener(e -> cari.run());
        jenisFilter.addActionListener(e -> { if (!resetting[0]) cari.run(); });
        add.addActionListener(e -> addTransaction());
        cat.addActionListener(e -> addCategory());

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(actionBar, BorderLayout.NORTH);
        centerPanel.add(Theme.tableScroll(table), BorderLayout.CENTER);

        p.add(Theme.heading("Riwayat Transaksi Keuangan"), BorderLayout.NORTH);
        p.add(centerPanel, BorderLayout.CENTER);

        loadTransactions(table, "", null);
        refreshTasks.add(() -> {
            resetting[0] = true; search.setText(""); jenisFilter.setSelectedIndex(0); resetting[0] = false;
            loadTransactions(table, "", null);
        });
        return p;
    }

    private String jenisFilterKeDb(JComboBox<String> combo) {
        return switch (combo.getSelectedIndex()) {
            case 1 -> "PEMASUKAN";
            case 2 -> "PENGELUARAN";
            default -> null;
        };
    }

    private void loadTransactions(JTable table, String keyword, String jenis) {
        try {
            DefaultTableModel m = model("ID", "Tanggal & Waktu", "Jenis", "Dompet", "Kategori", "Nominal", "Keterangan");
            for (var r : dao.riwayat(user.getIdPengguna(), keyword, jenis, 300)) {
                m.addRow(new Object[]{r.id(), r.tanggal().format(dateFormatter), r.jenis(),
                        r.dompet(), r.kategori(), money(r.nominal()), r.keterangan()});
            }
            table.setModel(m);
            hideIdColumn(table);
            setColumnWidths(table, 195, 150, 125, 130, 150, 150);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addTransaction() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.isEmpty()) { info("Buat dompet terlebih dahulu."); return; }

            JComboBox<String> type = new JComboBox<>(new String[]{"PEMASUKAN", "PENGELUARAN"});
            JComboBox<String> wallet = combo(wallets.stream().map(w -> w.nama() + " (" + money(w.saldo()) + ")").toList());
            JTextField nominal = Theme.rupiahField(null), ket = new JTextField();
            JComboBox<KeuanganDAO.KategoriRow> kategori = kategoriCombo();
            JDateChooser dateChooser = buatDateChooser(LocalDate.now());

            Runnable loadCats = () -> {
                kategori.removeAllItems();
                try {
                    for (var k : dao.kategori(user.getIdPengguna(), (String) type.getSelectedItem())) kategori.addItem(k);
                } catch (Exception ex) { error(ex); }
            };
            loadCats.run();
            type.addActionListener(e -> loadCats.run());

            if (Dialogs.showConfirmDialog(this, new Object[]{
                    "Jenis Transaksi:", type, "Pilih Dompet:", wallet, "Nominal (Rp):", nominal,
                    "Pilih Tanggal:", dateChooser, "Kategori:", kategori, "Catatan/Keterangan:", ket},
                    "Catat Transaksi", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            if (wallet.getSelectedIndex() < 0 || kategori.getSelectedItem() == null) {
                info("Pastikan dompet dan kategori telah dipilih."); return;
            }
            LocalDate parsedDate = ambilTanggal(dateChooser);
            long kid = ((KeuanganDAO.KategoriRow) kategori.getSelectedItem()).id();
            long wid = wallets.get(wallet.getSelectedIndex()).id();
            // [OOP - Polymorphism]: Referensi Transaksi dapat diisi objek Pemasukan atau Pengeluaran sebelum dikirim ke DAO.
            Transaksi tr = "PEMASUKAN".equals(type.getSelectedItem())
                    ? new Pemasukan(wid, kid, amount(nominal.getText()), parsedDate, ket.getText())
                    : new Pengeluaran(wid, kid, amount(nominal.getText()), parsedDate, ket.getText());
            dao.tambahTransaksi(user.getIdPengguna(), tr);
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void addCategory() {
        JTextField n = new JTextField();
        Theme.filterNoDigits(n);
        JComboBox<String> t = new JComboBox<>(new String[]{"PEMASUKAN", "PENGELUARAN"});
        if (Dialogs.showConfirmDialog(this, new Object[]{"Nama Kategori:", n, "Jenis Transaksi:", t},
                "Tambah Kategori Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (n.getText().trim().matches(".*\\d.*")) throw new IllegalArgumentException("Nama kategori tidak boleh mengandung angka.");
            dao.tambahKategori(user.getIdPengguna(), n.getText(), (String) t.getSelectedItem());
            info("Kategori berhasil ditambahkan.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void transfer() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.size() < 2) { info("Minimal harus ada dua dompet aktif untuk melakukan transfer."); return; }

            List<String> items = wallets.stream().map(w -> w.nama() + " (" + money(w.saldo()) + ")").toList();
            JComboBox<String> src = combo(items, 0), dst = combo(items, 1);
            JTextField nominal = Theme.rupiahField(null), ket = new JTextField();

            if (Dialogs.showConfirmDialog(this, new Object[]{
                    "Dompet Sumber (Dari):", src, "Dompet Tujuan (Ke):", dst,
                    "Nominal Transfer (Rp):", nominal, "Catatan/Keterangan:", ket},
                    "Konfirmasi Transfer", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            if (src.getSelectedIndex() < 0 || dst.getSelectedIndex() < 0) { info("Pilih dompet sumber dan tujuan terlebih dahulu."); return; }
            long sourceId = wallets.get(src.getSelectedIndex()).id(), destId = wallets.get(dst.getSelectedIndex()).id();
            if (sourceId == destId) { info("Dompet sumber dan dompet tujuan tidak boleh sama."); return; }

            BigDecimal jumlah = amount(nominal.getText());
            String catatan = ket.getText();
            dao.transfer(user.getIdPengguna(), sourceId, destId, jumlah, catatan);

            String bukti = """
                    ========================================
                               BUKTI TRANSFER DOMPETKU      
                    ========================================
                    Tanggal  : %s
                    Dari     : %s
                    Tujuan   : %s
                    Nominal  : %s
                    Catatan  : %s
                    Status   : BERHASIL & TEREKAM DI RIWAYAT
                    ========================================""".formatted(
                    LocalDate.now(), src.getSelectedItem(), dst.getSelectedItem(), money(jumlah), catatan);
            JTextArea areaBukti = new JTextArea(bukti);
            areaBukti.setEditable(false);
            areaBukti.setFont(new Font("Monospaced", Font.PLAIN, 13));
            Dialogs.showConfirmDialog(this, new JScrollPane(areaBukti), "Transfer Berhasil - Cetak Bukti",
                    JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    // ================= ANGGARAN =================

    private JPanel budgets() {
        JPanel p = Theme.page();
        JTable table = new JTable();
        Theme.styleTable(table);

        JButton add = Theme.button("+ Set Anggaran");
        add.setPreferredSize(new Dimension(150, 40));
        add.addActionListener(e -> addBudget());

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = Theme.heading("Anggaran Pengeluaran Bulanan");
        title.setFont(Theme.font(Font.BOLD, 23));
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(Theme.label("Pantau batas pengeluaran dan penggunaan anggaran setiap kategori.", Font.PLAIN, 13, Theme.MUTED));

        JPanel header = new JPanel(new BorderLayout(10, 8));
        header.setOpaque(false);
        header.add(titlePanel, BorderLayout.CENTER);
        header.add(Theme.buttonRow(add), BorderLayout.EAST);

        JPanel infoBox = Theme.borderedCard(new Color(255, 248, 225), new Color(235, 210, 145), 12, 15);
        infoBox.setLayout(new BorderLayout(10, 0));
        infoBox.add(Theme.label("ⓘ", Font.BOLD, 20, new Color(150, 105, 20)), BorderLayout.WEST);
        infoBox.add(Theme.label("<html><b>Informasi Anggaran</b><br>Hijau menunjukkan anggaran masih tersedia. Kuning menandakan penggunaan sudah mencapai 80%. Merah menunjukkan anggaran telah habis atau terlampaui.</html>",
                Font.PLAIN, 12, new Color(105, 78, 25)), BorderLayout.CENTER);

        JPanel tableCard = Theme.borderedCard(Theme.WHITE, Theme.BORDER, 8, 8);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(Theme.tableScroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(infoBox, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);

        p.add(header, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        loadBudgets(table);
        refreshTasks.add(() -> loadBudgets(table));
        return p;
    }

    private String formatTanggalIndonesia(String tanggal) {
        if (tanggal == null || tanggal.isBlank()) return "-";
        try {
            return LocalDate.parse(tanggal).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")));
        } catch (Exception e) {
            return tanggal;
        }
    }

    private String teksPenggunaan(BigDecimal limit, BigDecimal used) {
        if (limit.signum() == 0) return "-";
        String persen = used.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP).toPlainString() + "%";
        return used.compareTo(limit) > 0
                ? persen + " • Lebih " + money(used.subtract(limit))
                : persen + " • Sisa " + money(limit.subtract(used));
    }

    private void loadBudgets(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Kategori", "Mulai", "Selesai", "Batas Anggaran", "Terpakai", "Penggunaan");
            for (String[] r : dao.anggaran(user.getIdPengguna())) {
                BigDecimal limit = new BigDecimal(r[4]), used = new BigDecimal(r[5]);
                m.addRow(new Object[]{r[0], r[1], formatTanggalIndonesia(r[2]), formatTanggalIndonesia(r[3]),
                        money(limit), money(used), teksPenggunaan(limit, used)});
            }
            table.setModel(m);
            hideIdColumn(table);
            setColumnWidths(table, 120, 105, 105, 130, 130, 250);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addBudget() {
        try {
            var cats = dao.kategori(user.getIdPengguna(), "PENGELUARAN");
            if (cats.isEmpty()) { info("Tambahkan kategori pengeluaran terlebih dahulu."); return; }
            JComboBox<KeuanganDAO.KategoriRow> c = kategoriCombo(cats);
            JDateChooser mulai = buatDateChooser(YearMonth.now().atDay(1));
            JDateChooser akhir = buatDateChooser(YearMonth.now().atEndOfMonth());
            JTextField batas = Theme.rupiahField(null);

            if (Dialogs.showConfirmDialog(this, new Object[]{
                    "Kategori Pengeluaran:", c, "Tanggal Mulai:", mulai, "Tanggal Selesai:", akhir, "Batas Anggaran (Rp):", batas},
                    "Tambah Anggaran Pengeluaran", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            var selected = (KeuanganDAO.KategoriRow) c.getSelectedItem();
            if (selected == null) { info("Pilih kategori pengeluaran terlebih dahulu."); return; }
            dao.tambahAnggaran(user.getIdPengguna(), selected.id(), ambilTanggal(mulai), ambilTanggal(akhir), amount(batas.getText()));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    // ================= TARGET TABUNGAN =================

    private JPanel savings() {
        JPanel p = Theme.page();
        p.setLayout(new BorderLayout(0, 16));

        JButton add = Theme.button("+ Buat Impian"), con = Theme.buttonDark("+ Tambah Setoran");
        add.addActionListener(e -> addTarget());
        con.addActionListener(e -> contribute());

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(Theme.heading("Target Tabungan & Impian"));
        titlePanel.add(Theme.label("Wujudkan impianmu dengan perencanaan tabungan yang terarah.", Font.PLAIN, 12, Theme.HINT));

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setOpaque(false);
        header.add(titlePanel, BorderLayout.WEST);
        header.add(Theme.buttonRow(con, add), BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBorder(new EmptyBorder(5, 0, 15, 5));

        JScrollPane scroll = Theme.scroll(cards);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        p.add(scroll, BorderLayout.CENTER);

        Runnable reload = () -> loadTargetCards(cards);
        reload.run();
        refreshTasks.add(reload);
        return p;
    }

    private void loadTargetCards(JPanel container) {
        container.removeAll();
        try {
            List<String[]> targets = dao.target(user.getIdPengguna());
            if (targets.isEmpty()) {
                JPanel empty = Theme.card();
                empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel icon = Theme.label("✦", Font.BOLD, 38, Theme.GOLD);
                JLabel title = Theme.label("Belum Ada Impian", 19, true);
                JLabel desc = Theme.label("Mulai buat target tabungan pertamamu dan wujudkan impianmu.", Font.PLAIN, 13, Theme.HINT);
                for (JComponent c : new JComponent[]{icon, title, desc}) c.setAlignmentX(Component.CENTER_ALIGNMENT);

                empty.add(Box.createVerticalStrut(25)); empty.add(icon);
                empty.add(Box.createVerticalStrut(12)); empty.add(title);
                empty.add(Box.createVerticalStrut(8)); empty.add(desc);
                empty.add(Box.createVerticalStrut(25));
                container.add(empty);
            } else {
                for (String[] r : targets) {
                    container.add(createTargetCard(r));
                    container.add(Box.createVerticalStrut(15));
                }
            }
        } catch (Exception e) {
            error(e);
        }
        container.revalidate();
        container.repaint();
    }

    private JPanel createTargetCard(String[] r) {
        BigDecimal target = new BigDecimal(r[2]), terkumpul = new BigDecimal(r[3]);
        BigDecimal sisa = target.subtract(terkumpul).max(BigDecimal.ZERO);
        double persen = target.signum() <= 0 ? 0
                : Math.max(0, Math.min(100, terkumpul.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP).doubleValue()));

        LocalDate tenggat = tanggalAtauNull(r[5]), pertama = tanggalAtauNull(r[6]), terakhir = tanggalAtauNull(r[7]);
        int jumlahSetoran = r[8] == null ? 0 : Integer.parseInt(r[8]);
        boolean tercapai = terkumpul.compareTo(target) >= 0;

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 15));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));

        JPanel namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.add(Theme.label("✦  " + r[1], Font.BOLD, 18, Theme.CHARCOAL));
        namePanel.add(Box.createVerticalStrut(5));
        namePanel.add(Theme.label(tercapai ? "✓ TARGET TERCAPAI" : "● SEDANG BERJALAN", Font.BOLD, 10, tercapai ? Theme.INCOME : Theme.GOLD));

        JPanel top = new JPanel(new BorderLayout(10, 5));
        top.setOpaque(false);
        top.add(namePanel, BorderLayout.WEST);
        top.add(Theme.label(String.format(Locale.forLanguageTag("id-ID"), "%.1f%%", persen), Font.BOLD, 22, Theme.GOLD), BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JProgressBar progress = new JProgressBar(0, 1000);
        progress.setValue((int) Math.round(persen * 10));
        progress.setStringPainted(false);
        progress.setForeground(tercapai ? Theme.INCOME : Theme.GOLD);
        progress.setBackground(new Color(235, 229, 211));
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(100, 12));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(Theme.label(money(terkumpul), Font.BOLD, 23, Theme.INCOME));
        middle.add(Box.createVerticalStrut(3));
        middle.add(Theme.label("dari target " + money(target), Font.PLAIN, 12, Theme.HINT));
        middle.add(Box.createVerticalStrut(12));
        middle.add(progress);
        card.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 3, 12, 0));
        bottom.setOpaque(false);
        String estimasi = estimasiTercapai(target, terkumpul, pertama, terakhir, jumlahSetoran, tenggat);
        String rekomendasi = rekomendasiNabung(target, terkumpul, tenggat);
        String deadline = tenggat == null ? "Belum ditentukan"
                : tenggat.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")));
        bottom.add(createTargetInfo("SISA TARGET", money(sisa)));
        bottom.add(createTargetInfo("ESTIMASI TERCAPAI", tercapai ? "Sudah tercapai" : estimasi));
        bottom.add(createTargetInfo("TENGGAT", deadline));

        JPanel recommendation = new JPanel();
        recommendation.setOpaque(true);
        recommendation.setBackground(new Color(248, 243, 225));
        recommendation.setBorder(new EmptyBorder(10, 12, 10, 12));
        recommendation.setLayout(new BoxLayout(recommendation, BoxLayout.Y_AXIS));
        recommendation.add(Theme.label("✦  REKOMENDASI MENABUNG", Font.BOLD, 10, Theme.GOLD));
        recommendation.add(Box.createVerticalStrut(5));
        recommendation.add(Theme.label(tercapai ? "Impianmu sudah terwujud!" : rekomendasi, Font.BOLD, 13, Theme.CHARCOAL));

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));
        footer.add(recommendation, BorderLayout.CENTER);
        // Catatan: info grid diletakkan di atas kotak rekomendasi agar tata letak tetap ringkas
        JPanel southBox = new JPanel(new BorderLayout());
        southBox.setOpaque(false);
        southBox.add(bottom, BorderLayout.NORTH);
        southBox.add(footer, BorderLayout.SOUTH);
        card.add(southBox, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createTargetInfo(String title, String value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Theme.label(title, Font.BOLD, 9, Theme.HINT));
        panel.add(Box.createVerticalStrut(5));
        panel.add(Theme.label(value, Font.BOLD, 11, Theme.CHARCOAL));
        return panel;
    }

    private String estimasiTercapai(BigDecimal target, BigDecimal terkumpul, LocalDate pertama,
                                    LocalDate terakhir, int jumlahHariSetoran, LocalDate tenggat) {
        if (terkumpul.compareTo(target) >= 0) return "Sudah tercapai";
        if (pertama == null || terakhir == null || jumlahHariSetoran < 2) return "Butuh min. 2 kali setoran";
        long rentang = ChronoUnit.DAYS.between(pertama, terakhir);
        if (rentang <= 0) return "Butuh setoran di hari berbeda";

        double jarakRata = (double) rentang / (jumlahHariSetoran - 1);
        double perHari = terkumpul.doubleValue() / (rentang + jarakRata);
        if (perHari <= 0) return "Belum bisa diperkirakan";

        double hariButuh = Math.ceil(target.subtract(terkumpul).doubleValue() / perHari);
        if (hariButuh > 36500) return "Lebih dari 100 tahun";

        LocalDate hariIni = LocalDate.now();
        LocalDate estimasi = (terakhir.isAfter(hariIni) ? terakhir : hariIni).plusDays((long) hariButuh);
        String teks = estimasi.format(bulanFormatter);
        return (tenggat != null && estimasi.isAfter(tenggat)) ? teks + " (lewat tenggat)" : teks;
    }

    private String rekomendasiNabung(BigDecimal target, BigDecimal terkumpul, LocalDate tenggat) {
        BigDecimal sisa = target.subtract(terkumpul);
        if (sisa.signum() <= 0) return "-";
        if (tenggat == null) return "Tenggat belum diatur";
        long hari = ChronoUnit.DAYS.between(LocalDate.now(), tenggat);
        if (hari <= 0) return "Tenggat lewat, sisa " + money(sisa);
        BigDecimal perHari = sisa.divide(BigDecimal.valueOf(hari), 0, RoundingMode.CEILING);
        if (hari < 30) return money(perHari) + "/hari";
        long bulan = Math.max(1, Math.round(hari / 30.0));
        return money(perHari) + "/hari atau " + money(sisa.divide(BigDecimal.valueOf(bulan), 0, RoundingMode.CEILING)) + "/bulan";
    }

    private LocalDate tanggalAtauNull(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    private void addTarget() {
        JTextField n = new JTextField();
        Theme.filterNoDigits(n);
        JTextField v = Theme.rupiahField(null);
        JDateChooser d = buatDateChooser(LocalDate.now().plusMonths(6));

        if (Dialogs.showConfirmDialog(this, new Object[]{"Nama Impian/Target:", n, "Target Nominal (Rp):", v, "Tenggat Tanggal:", d},
                "Buat Target Tabungan Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (n.getText().trim().matches(".*\\d.*")) throw new IllegalArgumentException("Nama target tidak boleh mengandung angka.");
            dao.tambahTarget(user.getIdPengguna(), n.getText(), amount(v.getText()), ambilTanggal(d));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void contribute() {
        try {
            var targets = dao.target(user.getIdPengguna());
            if (targets.isEmpty()) { info("Buat target tabungan terlebih dahulu."); return; }

            JComboBox<String> c = combo(targets.stream().map(r -> r[1] + " (" + r[4] + ")").toList());
            JTextField n = Theme.rupiahField(null), k = new JTextField();

            if (Dialogs.showConfirmDialog(this, new Object[]{"Target Impian:", c, "Nominal Setoran (Rp):", n, "Keterangan/Sumber Dana:", k},
                    "Tambah Setoran Tabungan", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            if (c.getSelectedIndex() < 0) { info("Pilih target tabungan terlebih dahulu."); return; }
            dao.kontribusi(user.getIdPengguna(), Long.parseLong(targets.get(c.getSelectedIndex())[0]), amount(n.getText()), k.getText());
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    // ================= UTANG PIUTANG =================

    private JPanel debts() {
        JPanel p = Theme.page();
        JTable table = new JTable();
        Theme.styleTable(table);
        JButton add = Theme.button("+ Catat Utang/Piutang"), pay = Theme.buttonDark("+ Bayar / Cicil");
        add.addActionListener(e -> addDebt());
        pay.addActionListener(e -> payDebt());
        p.add(Theme.pageHeader("Catatan Utang & Piutang", Theme.buttonRow(pay, add)), BorderLayout.NORTH);
        p.add(Theme.tableScroll(table), BorderLayout.CENTER);
        loadDebts(table);
        refreshTasks.add(() -> loadDebts(table));
        return p;
    }

    private void loadDebts(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Jenis", "Nama Pihak", "Nominal Awal", "Sudah Dibayar", "Jatuh Tempo", "Status");
            for (String[] r : dao.utang(user.getIdPengguna())) {
                m.addRow(new Object[]{r[0], r[1], r[2], money(new BigDecimal(r[3])), money(new BigDecimal(r[4])), r[5], r[6]});
            }
            table.setModel(m);
            hideIdColumn(table);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addDebt() {
        JComboBox<String> jenis = new JComboBox<>(new String[]{"UTANG", "PIUTANG"});
        JTextField pihak = new JTextField(), nom = Theme.rupiahField(null);
        Theme.filterNoDigits(pihak);
        JDateChooser tempo = buatDateChooser(LocalDate.now().plusMonths(1));

        if (Dialogs.showConfirmDialog(this, new Object[]{
                "Jenis Catatan:", jenis, "Nama Pihak (Orang/Lembaga):", pihak, "Nominal Utama (Rp):", nom, "Jatuh Tempo:", tempo},
                "Catat Utang/Piutang Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (pihak.getText().trim().matches(".*\\d.*")) throw new IllegalArgumentException("Nama pihak tidak boleh mengandung angka.");
            dao.tambahUtang(user.getIdPengguna(), (String) jenis.getSelectedItem(), pihak.getText(), amount(nom.getText()), ambilTanggal(tempo));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void payDebt() {
        try {
            var rows = dao.utang(user.getIdPengguna());
            if (rows.isEmpty()) { info("Belum ada catatan utang/piutang."); return; }
            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.isEmpty()) { info("Buat dompet terlebih dahulu untuk mencatat pembayaran utang/piutang."); return; }

            JComboBox<String> c = combo(rows.stream().map(r -> r[1] + " - " + r[2] + " (" + r[6] + ")").toList());
            JComboBox<String> walletCombo = combo(wallets.stream().map(w -> w.nama() + " (" + money(w.saldo()) + ")").toList());
            JTextField n = Theme.rupiahField(null);

            if (Dialogs.showConfirmDialog(this, new Object[]{
                    "Pilih Catatan Utang/Piutang:", c, "Pilih Dompet:", walletCombo, "Nominal Pembayaran (Rp):", n},
                    "Catat Pembayaran / Angsuran", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            if (c.getSelectedIndex() < 0 || walletCombo.getSelectedIndex() < 0) {
                info("Pilih catatan utang/piutang dan dompet terlebih dahulu."); return;
            }
            dao.bayarUtang(user.getIdPengguna(), Long.parseLong(rows.get(c.getSelectedIndex())[0]),
                    amount(n.getText()), wallets.get(walletCombo.getSelectedIndex()).id());
            info("Pembayaran dicatat dan saldo dompet diperbarui.");
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    // ================= LAPORAN & EKSPOR PDF =================

    private JPanel reports() {
        JPanel p = Theme.page();
        JDateChooser start = buatDateChooser(YearMonth.now().atDay(1)), end = buatDateChooser(YearMonth.now().atEndOfMonth());
        start.setPreferredSize(new Dimension(150, 34));
        end.setPreferredSize(new Dimension(150, 34));

        JTextArea out = new JTextArea(18, 60);
        out.setEditable(false);
        out.setFont(new Font("Monospaced", Font.PLAIN, 13));
        out.setMargin(new Insets(16, 16, 16, 16));

        CategoryPiePanel incomeChart = new CategoryPiePanel("RINCIAN PEMASUKAN");
        CategoryPiePanel expenseChart = new CategoryPiePanel("RINCIAN PENGELUARAN");
        JButton show = Theme.button("Tampilkan Laporan"), pdf = Theme.buttonDark("Ekspor File PDF");

        show.addActionListener(e -> {
            try {
                LocalDate mulai = ambilTanggal(start), akhir = ambilTanggal(end);
                var ringkasan = dao.ringkasan(user.getIdPengguna(), mulai, akhir);
                var katIn = dao.rincianKategori(user.getIdPengguna(), "PEMASUKAN", mulai, akhir);
                var katOut = dao.rincianKategori(user.getIdPengguna(), "PENGELUARAN", mulai, akhir);
                incomeChart.setData(katIn);
                expenseChart.setData(katOut);
                var tren = dao.trenBulanan(user.getIdPengguna(), mulai, akhir);

                StringBuilder lap = new StringBuilder();
                lap.append("====================================================\n")
                        .append("               LAPORAN KEUANGAN DOMPETKU             \n")
                        .append("====================================================\n")
                        .append("Periode : ").append(mulai).append(" s.d. ").append(akhir).append("\n\n")
                        .append("RINGKASAN KEUANGAN\n----------------------------------------------------\n")
                        .append("Total Pemasukan   : ").append(money(ringkasan.pemasukan())).append("\n")
                        .append("Total Pengeluaran : ").append(money(ringkasan.pengeluaran())).append("\n")
                        .append("Selisih Bersih    : ").append(money(ringkasan.selisih())).append("\n\n");
                appendCategorySection(lap, "RINCIAN PEMASUKAN BERDASARKAN KATEGORI", "Belum ada data pemasukan.", katIn);
                appendCategorySection(lap, "RINCIAN PENGELUARAN BERDASARKAN KATEGORI", "Belum ada data pengeluaran.", katOut);
                lap.append("\nTREN KEUANGAN BULANAN\n----------------------------------------------------\n");
                if (tren.isEmpty()) lap.append("Belum ada data tren keuangan.\n");
                else for (var item : tren) {
                    lap.append(item.bulan()).append("\n  Pemasukan   : ").append(money(item.pemasukan()))
                            .append("\n  Pengeluaran : ").append(money(item.pengeluaran())).append("\n\n");
                }
                out.setText(lap.append("====================================================\n").toString());
                out.setCaretPosition(0);
            } catch (Exception ex) { error(ex); }
        });

        pdf.addActionListener(e -> {
            try {
                LocalDate mulai = ambilTanggal(start), akhir = ambilTanggal(end);
                File downloads = new File(System.getProperty("user.home"), "Downloads");
                if (!downloads.exists()) downloads.mkdirs();
                JFileChooser fc = new JFileChooser(downloads);
                fc.setDialogTitle("Simpan Laporan DompetKu");
                fc.setFileFilter(new FileNameExtensionFilter("Dokumen PDF (*.pdf)", "pdf"));
                fc.setSelectedFile(new File(downloads, "laporan-dompetku-" + mulai + "-sampai-" + akhir + ".pdf"));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File target = fc.getSelectedFile();
                    if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) target = new File(target.getAbsolutePath() + ".pdf");
                    eksporLaporanPdf(target, mulai, akhir);
                    info("Laporan PDF berhasil diekspor!\n\nLokasi penyimpanan:\n" + target.getAbsolutePath());
                }
            } catch (Exception ex) { error(ex); }
        });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.setOpaque(false);
        filterBar.add(Theme.label("Mulai:", 14, true)); filterBar.add(start);
        filterBar.add(Theme.label("Selesai:", 14, true)); filterBar.add(end);
        filterBar.add(show); filterBar.add(pdf);

        JPanel chartContainer = new JPanel(new GridLayout(1, 2, 16, 0));
        chartContainer.setBackground(Theme.WHITE);
        chartContainer.setBorder(new EmptyBorder(0, 0, 5, 0));
        chartContainer.add(incomeChart); chartContainer.add(expenseChart);

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(Theme.WHITE);
        JScrollPane textScroll = new JScrollPane(out);
        textScroll.setPreferredSize(new Dimension(900, 400));
        resultPanel.add(textScroll);
        resultPanel.add(Box.createVerticalStrut(16));
        resultPanel.add(chartContainer);

        JScrollPane scroll = new JScrollPane(resultPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel reportWrapper = new JPanel(new BorderLayout(0, 10));
        reportWrapper.setOpaque(false);
        reportWrapper.add(filterBar, BorderLayout.NORTH);
        reportWrapper.add(scroll, BorderLayout.CENTER);

        p.add(Theme.heading("Laporan Keuangan & Analisis"), BorderLayout.NORTH);
        p.add(reportWrapper, BorderLayout.CENTER);
        return p;
    }

    private void appendCategorySection(StringBuilder sb, String title, String emptyMsg, List<KeuanganDAO.KategoriLaporan> list) {
        sb.append("\n").append(title).append("\n----------------------------------------------------\n");
        if (list.isEmpty()) sb.append(emptyMsg).append("\n");
        else for (var item : list) {
            sb.append(item.nama()).append(" : ").append(money(item.total()))
                    .append(" (").append(item.persentase().toPlainString()).append("%)\n");
        }
    }

    private void eksporLaporanPdf(File file, LocalDate mulai, LocalDate akhir) throws Exception {
        if (akhir.isBefore(mulai)) throw new IllegalArgumentException("Tanggal selesai tidak boleh lebih awal dari tanggal mulai.");
        var ringkasan = dao.ringkasan(user.getIdPengguna(), mulai, akhir);
        var katIn = dao.rincianKategori(user.getIdPengguna(), "PEMASUKAN", mulai, akhir);
        var katOut = dao.rincianKategori(user.getIdPengguna(), "PENGELUARAN", mulai, akhir);
        var riwayatSemua = dao.riwayat(user.getIdPengguna(), "", 1000);

        Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            var subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            var secFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            var headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            var bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph title = new Paragraph("LAPORAN KEUANGAN DOMPETKU", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            Paragraph meta = new Paragraph("Pemilik Akun: " + user.getNama() + " (@" + user.getUsername() + ")\n"
                    + "Periode Laporan: " + mulai + " s.d. " + akhir + "\n\n", subFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            doc.add(meta);

            addPdfSection(doc, "1. Ringkasan Keuangan", new PdfPTable(2), secFont, headFont, bodyFont,
                    new String[]{"Keterangan", "Jumlah"},
                    List.of(new String[]{"Total Pemasukan", money(ringkasan.pemasukan())},
                            new String[]{"Total Pengeluaran", money(ringkasan.pengeluaran())},
                            new String[]{"Selisih Bersih / Cashflow", money(ringkasan.selisih())}), null);

            addPdfSection(doc, "2. Rincian Pemasukan per Kategori", new PdfPTable(3), secFont, headFont, bodyFont,
                    new String[]{"Kategori", "Total", "Persentase"},
                    katIn.stream().map(i -> new String[]{i.nama(), money(i.total()), i.persentase().toPlainString() + "%"}).toList(),
                    new String[]{"Belum ada data pemasukan", "-", "-"});

            addPdfSection(doc, "3. Rincian Pengeluaran per Kategori", new PdfPTable(3), secFont, headFont, bodyFont,
                    new String[]{"Kategori", "Total", "Persentase"},
                    katOut.stream().map(i -> new String[]{i.nama(), money(i.total()), i.persentase().toPlainString() + "%"}).toList(),
                    new String[]{"Belum ada data pengeluaran", "-", "-"});

            List<String[]> trxRows = riwayatSemua.stream()
                    .filter(r -> { LocalDate t = r.tanggal().toLocalDate(); return !t.isBefore(mulai) && !t.isAfter(akhir); })
                    .map(r -> new String[]{r.tanggal().toLocalDate().toString(), r.jenis(), r.dompet(), r.kategori(),
                            money(r.nominal()), r.keterangan() == null ? "-" : r.keterangan()})
                    .toList();
            addPdfSection(doc, "4. Daftar Transaksi Periode Terpilih", new PdfPTable(new float[]{2.2f, 1.8f, 2f, 2.2f, 2.2f, 3f}),
                    secFont, headFont, bodyFont,
                    new String[]{"Tanggal", "Jenis", "Dompet", "Kategori", "Nominal", "Keterangan"},
                    trxRows, new String[]{"-", "-", "Tidak ada transaksi pada periode ini", "-", "-", "-"});

            doc.close();
        }
    }

    private void addPdfSection(Document doc, String title, PdfPTable tbl, com.lowagie.text.Font secFont,
                               com.lowagie.text.Font headFont, com.lowagie.text.Font bodyFont,
                               String[] headers, List<String[]> rows, String[] emptyRow) throws DocumentException {
        doc.add(new Paragraph(title, secFont));
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(6);
        tbl.setSpacingAfter(14);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(45, 45, 45));
            cell.setPadding(6);
            tbl.addCell(cell);
        }
        List<String[]> data = rows.isEmpty() && emptyRow != null ? List.<String[]>of(emptyRow) : rows;
        for (String[] row : data) {
            for (String v : row) {
                PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, bodyFont));
                cell.setPadding(5);
                tbl.addCell(cell);
            }
        }
        doc.add(tbl);
    }

    // ================= HELPER KOMPONEN & FORMAT =================

    private JDateChooser buatDateChooser(LocalDate initialDate) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd MMMM yyyy");
        chooser.setFont(Theme.font(Font.PLAIN, 14));
        chooser.setPreferredSize(new Dimension(250, 40));
        chooser.setDate(Date.from((initialDate != null ? initialDate : LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        chooser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 212), 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        chooser.setBackground(Color.WHITE);

        JButton calBtn = chooser.getCalendarButton();
        Color normal = new Color(20, 83, 45), hover = new Color(11, 56, 32);
        calBtn.setBackground(normal);
        calBtn.setForeground(Color.WHITE);
        calBtn.setFocusPainted(false);
        calBtn.setBorderPainted(false);
        calBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calBtn.setPreferredSize(new Dimension(42, 36));
        calBtn.setToolTipText("Pilih tanggal");
        Theme.onHover(calBtn, h -> calBtn.setBackground(h ? hover : normal));
        return chooser;
    }

    private LocalDate ambilTanggal(JDateChooser chooser) {
        Date d = chooser.getDate();
        if (d == null) throw new IllegalArgumentException("Tanggal wajib dipilih melalui kalender.");
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void refreshAll() {
        loadSummary();
        refreshTasks.forEach(Runnable::run);
        revalidate();
        repaint();
    }

    private String money(BigDecimal n) { return rupiah.format(n == null ? BigDecimal.ZERO : n); }

    private BigDecimal parseNumber(String s) {
        return new BigDecimal(s.trim().replace("Rp", "").replace(" ", "").replace(".", "").replace(",", "."));
    }

    private BigDecimal amount(String s) {
        BigDecimal b = parseNumber(s);
        if (b.signum() <= 0) throw new IllegalArgumentException("Nominal harus lebih dari nol.");
        return b;
    }

    private BigDecimal amountOrZero(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        BigDecimal b = parseNumber(s);
        if (b.signum() < 0) throw new IllegalArgumentException("Saldo awal tidak boleh negatif.");
        return b;
    }

    private String plain(BigDecimal n) {
        return (n == null || n.signum() == 0) ? "0" : n.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void hideIdColumn(JTable table) {
        if (table.getColumnCount() > 0 && "ID".equals(table.getColumnName(0))) {
            table.removeColumn(table.getColumnModel().getColumn(0));
        }
    }

    private Long selectedId(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? null : Long.parseLong(table.getModel().getValueAt(table.convertRowIndexToModel(row), 0).toString());
    }

    DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private JComboBox<KeuanganDAO.KategoriRow> kategoriCombo() { return kategoriCombo(List.of()); }

    // [OOP - Polymorphism]: Method overloading kategoriCombo dan combo untuk variasi argumen default.
    private JComboBox<KeuanganDAO.KategoriRow> kategoriCombo(List<KeuanganDAO.KategoriRow> categories) {
        JComboBox<KeuanganDAO.KategoriRow> combo = new JComboBox<>();
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, val, idx, sel, foc);
                setText(val instanceof KeuanganDAO.KategoriRow k ? k.nama() : "");
                return this;
            }
        });
        categories.forEach(combo::addItem);
        return combo;
    }

    private JComboBox<String> combo(List<String> items) { return combo(items, 0); }

    private JComboBox<String> combo(List<String> items, int index) {
        JComboBox<String> c = new JComboBox<>(items.toArray(String[]::new));
        if (!items.isEmpty()) c.setSelectedIndex(Math.max(0, Math.min(index, items.size() - 1)));
        return c;
    }

    private void error(Exception e) {
        String msg = (e instanceof SQLException se && "23505".equals(se.getSQLState()))
                ? "Data dengan nama tersebut sudah ada. Gunakan nama lain."
                : e instanceof NumberFormatException ? "Format angka tidak valid. Contoh: 150.000"
                : e instanceof DateTimeParseException ? "Format tanggal tidak valid. Gunakan YYYY-MM-DD, contoh: 2026-11-30"
                : e.getMessage() == null ? e.toString() : e.getMessage();
        Dialogs.showMessageDialog(this, msg, "Kesalahan", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String s) { Dialogs.showMessageDialog(this, s); }
}
