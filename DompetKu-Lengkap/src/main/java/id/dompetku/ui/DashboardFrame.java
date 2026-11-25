package id.dompetku.ui;

import id.dompetku.dao.KeuanganDAO;
import id.dompetku.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFrame extends JFrame {

    private final Pengguna user;
    private final KeuanganDAO dao = new KeuanganDAO();
    private final NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));

    {
        rupiah.setMinimumFractionDigits(0);
        rupiah.setMaximumFractionDigits(0);
    }
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"));

    private final List<Runnable> refreshTasks = new ArrayList<>();

    private JLabel lblTotalSaldo = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblPemasukan = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblPengeluaran = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblSelisih = new JLabel("Rp 0", SwingConstants.LEFT);

    private JLabel lblStatusTarget = new JLabel("Memuat...", SwingConstants.LEFT);
    private JLabel lblStatusUtang = new JLabel("Memuat...", SwingConstants.LEFT);

    private final JTable tblRincian = new JTable();

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final ProfilePanel profilePanel;

    public DashboardFrame(Pengguna u) {
        super("DompetKu • Dashboard Keuangan");
        user = u;

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

        profilePanel = new ProfilePanel(user,
                () -> sidebar.setUser(user.getNama(), user.getUsername()));

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
        if (Sidebar.PROFILE_KEY.equals(page)) {
            profilePanel.reload();
        }

        cards.show(content, page);
        sidebar.select(page);
    }

    private void logout() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    private JPanel summary() {
        JPanel p = Theme.page();
        p.setLayout(new BorderLayout(0, 14));

        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        JLabel title = Theme.heading("Ringkasan Keuangan Bulanan");

        JPanel headerAndNotif = new JPanel(new BorderLayout(0, 8));
        headerAndNotif.setOpaque(false);
        headerAndNotif.add(title, BorderLayout.NORTH);
        headerAndNotif.add(createNotificationPanel(), BorderLayout.CENTER);

        topContainer.add(headerAndNotif, BorderLayout.NORTH);

        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsGrid.setOpaque(false);

        cardsGrid.add(createStatCard("Total Saldo", lblTotalSaldo, Theme.TEXT, Theme.WHITE));
        cardsGrid.add(createStatCard("Pemasukan Bulan Ini", lblPemasukan,
                new Color(15, 81, 50), new Color(212, 237, 218)));
        cardsGrid.add(createStatCard("Pengeluaran Bulan Ini", lblPengeluaran,
                new Color(114, 28, 36), new Color(248, 215, 218)));
        cardsGrid.add(createStatCard("Selisih / Cashflow", lblSelisih,
                Theme.GOLD, Theme.WHITE));

        JPanel statusWidgetsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        statusWidgetsGrid.setOpaque(false);
        statusWidgetsGrid.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel targetCard = createClickableStatCard("Status Target Tabungan",
                lblStatusTarget, Theme.TEXT, Theme.WHITE,
                () -> navigate("tabungan"));
        targetCard.setToolTipText("Klik untuk membuka halaman Target Tabungan");
        statusWidgetsGrid.add(targetCard);
        // Klik kartu ini untuk langsung membuka halaman Utang-Piutang
        statusWidgetsGrid.add(createClickableStatCard("Status Catatan Utang",
                lblStatusUtang, Theme.TEXT, Theme.WHITE,
                () -> navigate("utang")));

        JPanel statsWrapper = new JPanel(new BorderLayout(0, 0));
        statsWrapper.setOpaque(false);
        statsWrapper.add(cardsGrid, BorderLayout.NORTH);
        statsWrapper.add(statusWidgetsGrid, BorderLayout.CENTER);

        topContainer.add(statsWrapper, BorderLayout.CENTER);
        p.add(topContainer, BorderLayout.NORTH);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);

        JPanel walletDetailPanel = new JPanel(new BorderLayout(0, 6));
        walletDetailPanel.setOpaque(false);

        JLabel lblSub = new JLabel("Rincian Dompet & Akun");
        lblSub.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSub.setForeground(Theme.TEXT);

        walletDetailPanel.add(lblSub, BorderLayout.NORTH);

        styleTable(tblRincian);
        walletDetailPanel.add(createTableContainer(tblRincian), BorderLayout.CENTER);

        centerContainer.add(walletDetailPanel, BorderLayout.CENTER);
        centerContainer.add(createFinancialTipPanel(), BorderLayout.SOUTH);

        p.add(centerContainer, BorderLayout.CENTER);

        loadSummary();
        return p;
    }

    private JPanel createFinancialTipPanel() {
        JPanel tipPanel = new JPanel(new BorderLayout(12, 0));
        tipPanel.setBackground(Theme.WHITE);

        tipPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel iconLabel = new JLabel("💡");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JLabel textLabel = new JLabel(
                "<html><b>Tips Keuangan Mahasiswa:</b> Biasakan mencatat setiap pengeluaran sekecil apapun dan sisihkan minimal 10% uang saku bulanan di awal untuk pos tabungan darurat.</html>"
        );

        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        textLabel.setForeground(Theme.TEXT);

        tipPanel.add(iconLabel, BorderLayout.WEST);
        tipPanel.add(textLabel, BorderLayout.CENTER);

        return tipPanel;
    }

    private JPanel createNotificationPanel() {
        JPanel notifPanel = new JPanel();
        notifPanel.setLayout(new BoxLayout(notifPanel, BoxLayout.Y_AXIS));
        notifPanel.setOpaque(false);

        try {
            var utangList = dao.utang(user.getIdPengguna());
            LocalDate today = LocalDate.now();

            for (String[] u : utangList) {
                String status = u[6];

                if (!"LUNAS".equalsIgnoreCase(status) && u[5] != null) {
                    LocalDate dueDate = LocalDate.parse(u[5]);
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);

                    boolean piutang = "PIUTANG".equalsIgnoreCase(u[1]);
                    String pihak = (piutang ? "Piutang dari " : "Utang kepada ") + u[2];

                    if (daysBetween < 0) {
                        notifPanel.add(createAlertBox(
                                "⚠️ PERINGATAN: " + pihak
                                        + " telah LEWAT JATUH TEMPO (" + tgl(u[5]) + ")!",
                                Theme.EXPENSE));

                    } else if (daysBetween == 0) {
                        notifPanel.add(createAlertBox(
                                "🔔 PERINGATAN: " + pihak
                                        + " jatuh tempo HARI INI (" + tgl(u[5]) + ").",
                                Theme.GOLD));

                    } else if (daysBetween <= 3) {
                        notifPanel.add(createAlertBox(
                                "🔔 PERINGATAN: " + pihak
                                        + " akan jatuh tempo dalam " + daysBetween
                                        + " hari (" + tgl(u[5]) + ").",
                                Theme.GOLD));
                    }
                }
            }

            var anggaranList = dao.anggaran(user.getIdPengguna());

            for (String[] a : anggaranList) {
                BigDecimal limit = new BigDecimal(a[4]);
                BigDecimal used = new BigDecimal(a[5]);

                if (limit.signum() > 0) {
                    BigDecimal percentage = used.multiply(new BigDecimal("100"))
                            .divide(limit, 0, java.math.RoundingMode.HALF_UP);

                    if (percentage.compareTo(new BigDecimal("100")) >= 0) {
                        notifPanel.add(createAlertBox(
                                "🚨 ANGGARAN HABIS: Kategori '" + a[1]
                                        + "' telah mencapai " + percentage
                                        + "% dari batas anggaran!",
                                Theme.EXPENSE));

                    } else if (percentage.compareTo(new BigDecimal("80")) >= 0) {
                        notifPanel.add(createAlertBox(
                                "⚠️ PERINGATAN ANGGARAN: Kategori '" + a[1]
                                        + "' sudah terpakai " + percentage + "%!",
                                Theme.GOLD));
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return notifPanel;
    }

    private JPanel createAlertBox(String message, Color color) {
        boolean warning = color == Theme.GOLD;
        Color textColor = warning ? new Color(0x7A, 0x56, 0x00) : color;

        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(warning
                ? new Color(0xFF, 0xF4, 0xD6)
                : new Color(255, 240, 240));

        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(textColor);

        box.add(lbl, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 6, 0));
        wrapper.add(box, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createStatCard(String title, JLabel valueLabel,
                                  Color accentColor, Color backgroundColor) {

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(backgroundColor);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(Theme.TEXT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // Kartu statistik yang bisa diklik (kursor tangan + efek hover tipis)
    private JPanel createClickableStatCard(String title, JLabel valueLabel,
                                           Color accentColor, Color backgroundColor,
                                           Runnable onClick) {

        JPanel card = createStatCard(title, valueLabel, accentColor, backgroundColor);

        Color hoverColor = new Color(250, 246, 235);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Klik untuk membuka halaman Utang-Piutang");

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(backgroundColor);
            }
        });

        return card;
    }

    private void loadSummary() {
        try {
            DefaultTableModel rincian = model("Nama Dompet", "Jenis", "Saldo");
            BigDecimal total = BigDecimal.ZERO;

            for (var w : dao.dompet(user.getIdPengguna())) {
                rincian.addRow(new Object[]{
                        w.nama(), w.jenis(), money(w.saldo())
                });

                total = total.add(w.saldo());
            }

            tblRincian.setModel(rincian);

            var r = dao.ringkasan(
                    user.getIdPengguna(),
                    YearMonth.now().atDay(1),
                    YearMonth.now().atEndOfMonth()
            );

            lblTotalSaldo.setText(money(total));
            lblPemasukan.setText(money(r.pemasukan()));
            lblPengeluaran.setText(money(r.pengeluaran()));
            lblSelisih.setText(money(r.selisih()));

            var targets = dao.target(user.getIdPengguna());

            long activeTargets = targets.stream()
                    .filter(t -> !"TERCAPAI".equalsIgnoreCase(t[4]))
                    .count();

            lblStatusTarget.setText(targets.isEmpty()
                    ? "Belum ada target"
                    : activeTargets + " Target Aktif Berjalan");

            var debts = dao.utang(user.getIdPengguna());

            var unpaid = debts.stream()
                    .filter(d -> !"LUNAS".equalsIgnoreCase(d[6]))
                    .toList();

            if (debts.isEmpty()) {
                lblStatusUtang.setText("Tidak ada catatan");

            } else if (unpaid.isEmpty()) {
                lblStatusUtang.setText("Semua catatan sudah lunas");

            } else {
                final int maxShown = 3;

                StringBuilder sb = new StringBuilder("<html>")
                        .append(unpaid.size())
                        .append(" Catatan Belum Lunas")
                        .append("<div style='font-size:11px;font-weight:normal'>");

                for (int i = 0; i < Math.min(maxShown, unpaid.size()); i++) {
                    String[] d = unpaid.get(i);

                    // d[1]=jenis, d[2]=nama pihak, d[3]=nominal awal, d[4]=sudah dibayar
                    BigDecimal sisa = new BigDecimal(d[3]).subtract(new BigDecimal(d[4]));

                    sb.append("• ")
                            .append(escape(d[1])).append(" - ")
                            .append(escape(d[2]))
                            .append(" (sisa ").append(money(sisa)).append(")<br>");
                }

                if (unpaid.size() > maxShown) {
                    sb.append("+ ").append(unpaid.size() - maxShown).append(" lainnya...");
                }

                sb.append("</div></html>");
                lblStatusUtang.setText(sb.toString());
            }

        } catch (Exception ex) {
            error(ex);
        }
    }

    private JPanel wallets() {
        JPanel p = Theme.page();

        JTable table = new JTable();
        styleTable(table);

        JButton add = Theme.button("+ Tambah Dompet");
        JButton transfer = Theme.buttonDark("Transfer");
        JButton edit = Theme.buttonGhost("Edit");
        JButton delete = Theme.buttonDanger("Hapus");

        edit.setEnabled(false);
        delete.setEnabled(false);

        add.addActionListener(e -> addWallet());
        transfer.addActionListener(e -> transfer());
        edit.addActionListener(e -> editWallet(table));
        delete.addActionListener(e -> deleteWallet(table));

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            edit.setEnabled(selected);
            delete.setEnabled(selected);
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editWallet(table);
                }
            }
        });

        JLabel hint = new JLabel(
                "Pilih satu dompet di tabel untuk mengedit atau menghapusnya."
        );

        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(Theme.HINT);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 4, 0));

        toolbar.add(hint, BorderLayout.WEST);
        toolbar.add(Theme.buttonRow(edit, delete, transfer, add), BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(createTableContainer(table), BorderLayout.CENTER);

        p.add(Theme.pageHeader("Daftar Dompet & Rekening", null), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);

        loadWallets(table);
        refreshTasks.add(() -> loadWallets(table));

        return p;
    }

    private void loadWallets(JTable table) {
        try {
            var rows = dao.dompet(user.getIdPengguna());
            DefaultTableModel m = model("ID", "Nama Dompet", "Jenis", "Saldo");

            for (var r : rows) {
                m.addRow(new Object[]{
                        r.id(), r.nama(), r.jenis(), money(r.saldo())
                });
            }

            table.setModel(m);
            hideIdColumn(table);

        } catch (Exception e) {
            error(e);
        }
    }

    private void addWallet() {
        JTextField nama = new JTextField();
        JComboBox<String> jenis = new JComboBox<>(
                new String[]{"TUNAI", "BANK", "EWALLET"}
        );

        JTextField saldo = new JTextField("0");
        rupiahInput(saldo);
        allowOnly(nama, "[\\p{L}\\p{N} ]*", 30);
        rupiahInput(saldo);

        Object[] fields = {
                "Nama Dompet:", nama,
                "Jenis Akun:", jenis,
                "Saldo Awal (Rp):", saldo
        };

        if (Dialogs.showConfirmDialog(
                this, fields, "Tambah Dompet Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String namaBersih = nama.getText().trim();
            if (!namaBersih.matches(".*\\p{L}.*")) {
                info("Nama dompet harus berisi minimal satu huruf.");
                return;
            }

            BigDecimal s = amountOrZero(saldo.getText());

            Dompet d = switch ((String) jenis.getSelectedItem()) {
                case "BANK" -> new RekeningBank(user.getIdPengguna(), namaBersih, s);
                case "EWALLET" -> new EWallet(user.getIdPengguna(), namaBersih, s);
                default -> new DompetTunai(user.getIdPengguna(), namaBersih, s);
            };

            dao.tambahDompet(d);
            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private void editWallet(JTable table) {
        Long id = selectedId(table);

        if (id == null) {
            info("Pilih dompet yang ingin diedit terlebih dahulu.");
            return;
        }

        try {
            var w = dao.dompet(user.getIdPengguna()).stream()
                    .filter(x -> x.id() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Dompet tidak ditemukan. Muat ulang halaman."
                    ));

            JTextField nama = new JTextField(w.nama());
            allowOnly(nama, "[\\p{L}\\p{N} ]*", 30);

            JComboBox<String> jenis = new JComboBox<>(
                    new String[]{"TUNAI", "BANK", "EWALLET"}
            );

            jenis.setSelectedItem(w.jenis());

            JTextField saldoAwal = new JTextField(plain(w.saldoAwal()));
            rupiahInput(saldoAwal);

            JLabel note = new JLabel(
                    "<html><div style='width:260px;color:#7A6A55'>Saldo saat ini <b>"
                            + money(w.saldo())
                            + "</b>. Mengubah saldo awal akan ikut mengubah saldo saat ini.</div></html>"
            );

            Object[] fields = {
                    "Nama Dompet:", nama,
                    "Jenis Akun:", jenis,
                    "Saldo Awal (Rp):", saldoAwal,
                    note
            };

            if (Dialogs.showConfirmDialog(
                    this, fields, "Edit Dompet",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            dao.ubahDompet(
                    user.getIdPengguna(),
                    id,
                    nama.getText(),
                    (String) jenis.getSelectedItem(),
                    amountOrZero(saldoAwal.getText())
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private void deleteWallet(JTable table) {
        Long id = selectedId(table);

        if (id == null) {
            info("Pilih dompet yang ingin dihapus terlebih dahulu.");
            return;
        }

        try {
            var w = dao.dompet(user.getIdPengguna()).stream()
                    .filter(x -> x.id() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Dompet tidak ditemukan. Muat ulang halaman."
                    ));

            int riwayat = dao.jumlahRiwayatDompet(user.getIdPengguna(), id);

            StringBuilder msg = new StringBuilder("<html><div style='width:300px'>");

            if (riwayat == 0) {
                msg.append("Hapus dompet <b>")
                        .append(escape(w.nama()))
                        .append("</b> secara permanen?");
            } else {
                msg.append("Dompet <b>")
                        .append(escape(w.nama()))
                        .append("</b> punya ")
                        .append(riwayat)
                        .append(" catatan transaksi/transfer.<br><br>")
                        .append("Dompet akan disembunyikan dari daftar. ")
                        .append("Riwayat transaksinya tetap tersimpan.");
            }

            if (w.saldo().signum() != 0) {
                msg.append("<br><br>Saldo <b>")
                        .append(money(w.saldo()))
                        .append("</b> tidak akan ikut dihitung lagi di Total Saldo.");
            }

            msg.append("</div></html>");

            Object[] options = {"Hapus", "Batal"};

            int choice = Dialogs.showOptionDialog(
                    this,
                    new JLabel(msg.toString()),
                    "Hapus Dompet",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (choice != 0) return;

            dao.hapusDompet(user.getIdPengguna(), id);
            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel transactions() {
        JPanel p = Theme.page();

        JTable table = new JTable();
        styleTable(table);

        JTextField search = new JTextField(16);
        JComboBox<String> jenisFilter = new JComboBox<>(new String[]{"Semua", "Pemasukan", "Pengeluaran"});

        JButton find = Theme.button("Cari");
        JButton add = Theme.button("+ Catat Transaksi");
        JButton cat = Theme.buttonDark("+ Kelola Kategori");

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Pencarian:"));
        searchBar.add(search);
        searchBar.add(jenisFilter);
        searchBar.add(find);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);
        actionBtns.add(cat);
        actionBtns.add(add);

        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        actionBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        actionBar.add(searchBar, BorderLayout.WEST);
        actionBar.add(actionBtns, BorderLayout.EAST);

        find.addActionListener(e -> loadTransactions(table, search.getText(), (String) jenisFilter.getSelectedItem()));
        jenisFilter.addActionListener(e -> loadTransactions(table, search.getText(), (String) jenisFilter.getSelectedItem()));
        add.addActionListener(e -> addTransaction());
        cat.addActionListener(e -> addCategory());

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(actionBar, BorderLayout.NORTH);
        centerPanel.add(createTableContainer(table), BorderLayout.CENTER);

        p.add(Theme.heading("Riwayat Transaksi Keuangan"), BorderLayout.NORTH);
        p.add(centerPanel, BorderLayout.CENTER);

        loadTransactions(table, "", "Semua");
        refreshTasks.add(() -> { search.setText(""); jenisFilter.setSelectedItem("Semua"); loadTransactions(table, "", "Semua"); });

        return p;
    }

    private void loadTransactions(JTable table, String keyword, String filterJenis) {
        try {
            DefaultTableModel m = model(
                    "ID", "Tanggal & Waktu", "Jenis",
                    "Dompet", "Kategori", "Nominal", "Keterangan"
            );

            for (var r : dao.riwayat(user.getIdPengguna(), keyword, 300)) {
                if (!"Semua".equals(filterJenis)
                        && !String.valueOf(r.jenis()).equalsIgnoreCase(filterJenis)) {
                    continue;
                }
                m.addRow(new Object[] {
                        r.id(),
                        r.tanggal().format(dateFormatter),
                        r.jenis(),
                        r.dompet(),
                        r.kategori(),
                        money(r.nominal()),
                        r.keterangan()
                });
            }

            table.setModel(m);
            hideIdColumn(table);
            setColumnWidths(table, 195, 150, 125, 130, 150, 150);

        } catch (Exception e) {
            error(e);
        }
    }

    // ==========================================
    // CATAT TRANSAKSI - TANPA MENAMPILKAN ID
    // ==========================================

    private void addTransaction() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());

            if (wallets.isEmpty()) {
                info("Buat dompet terlebih dahulu.");
                return;
            }

            JComboBox<String> type = new JComboBox<>(
                    new String[]{"PEMASUKAN", "PENGELUARAN"}
            );

            // Hanya nama + saldo. ID tetap ada di list "wallets"
            // (urutannya sama dengan isi combo).
            JComboBox<String> wallet = combo(wallets.stream()
                    .map(w -> w.nama() + " (" + money(w.saldo()) + ")")
                    .toList());

            JTextField nominal = new JTextField();
            rupiahInput(nominal);
            JTextField ket = new JTextField();

            JComboBox<KeuanganDAO.KategoriRow> kategori = kategoriCombo();

            LocalDate now = LocalDate.now();

            String[] days = new String[31];

            for (int i = 0; i < 31; i++) {
                days[i] = String.format("%02d", i + 1);
            }

            JComboBox<String> comboDay = new JComboBox<>(days);
            comboDay.setSelectedItem(String.format("%02d", now.getDayOfMonth()));

            String[] months = {
                    "Januari", "Februari", "Maret", "April",
                    "Mei", "Juni", "Juli", "Agustus",
                    "September", "Oktober", "November", "Desember"
            };

            JComboBox<String> comboMonth = new JComboBox<>(months);
            comboMonth.setSelectedIndex(now.getMonthValue() - 1);

            int currentYear = now.getYear();

            String[] years = {
                    String.valueOf(currentYear),
                    String.valueOf(currentYear - 1),
                    String.valueOf(currentYear - 2)
            };

            JComboBox<String> comboYear = new JComboBox<>(years);

            JPanel datePanel = new JPanel(new GridLayout(1, 3, 5, 0));
            datePanel.setOpaque(false);
            datePanel.add(comboDay);
            datePanel.add(comboMonth);
            datePanel.add(comboYear);

            // Jumlah hari menyesuaikan bulan & tahun (28/29/30/31)
            Runnable syncDays = () -> {
                int month = comboMonth.getSelectedIndex() + 1;
                int year = Integer.parseInt((String) comboYear.getSelectedItem());
                int max = YearMonth.of(year, month).lengthOfMonth();

                int selected = Integer.parseInt((String) comboDay.getSelectedItem());

                DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
                for (int i = 1; i <= max; i++) {
                    m.addElement(String.format("%02d", i));
                }

                comboDay.setModel(m);
                comboDay.setSelectedItem(String.format("%02d", Math.min(selected, max)));
            };

            comboMonth.addActionListener(e -> syncDays.run());
            comboYear.addActionListener(e -> syncDays.run());
            syncDays.run();

            Runnable loadCats = () -> {
                kategori.removeAllItems();

                try {
                    for (var k : dao.kategori(
                            user.getIdPengguna(),
                            (String) type.getSelectedItem())) {
                        kategori.addItem(k);
                    }
                } catch (Exception ex) {
                    error(ex);
                }
            };

            loadCats.run();
            type.addActionListener(e -> loadCats.run());

            Object[] fields = {
                    "Jenis Transaksi:", type,
                    "Pilih Dompet:", wallet,
                    "Nominal (Rp):", nominal,
                    "Pilih Tanggal:", datePanel,
                    "Kategori:", kategori,
                    "Catatan/Keterangan:", ket
            };

            if (Dialogs.showConfirmDialog(
                    this, fields, "Catat Transaksi",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            if (wallet.getSelectedIndex() < 0
                    || kategori.getSelectedItem() == null) {
                info("Pastikan dompet dan kategori telah dipilih.");
                return;
            }

            KeuanganDAO.KategoriRow selectedKategori =
                    (KeuanganDAO.KategoriRow) kategori.getSelectedItem();

            long kid = selectedKategori.id();

            String selectedDay = (String) comboDay.getSelectedItem();
            String selectedMonth = String.format("%02d", comboMonth.getSelectedIndex() + 1);
            String selectedYear = (String) comboYear.getSelectedItem();

            LocalDate parsedDate = LocalDate.parse(
                    selectedYear + "-" + selectedMonth + "-" + selectedDay
            );

            // ID asli diambil dari list dompet berdasarkan urutan pilihan
            long wid = wallets.get(wallet.getSelectedIndex()).id();

            Transaksi tr = "PEMASUKAN".equals(type.getSelectedItem())
                    ? new Pemasukan(wid, kid, amount(nominal.getText()),
                    parsedDate, ket.getText())
                    : new Pengeluaran(wid, kid, amount(nominal.getText()),
                    parsedDate, ket.getText());

            dao.tambahTransaksi(user.getIdPengguna(), tr);
            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private void addCategory() {
        JTextField n = new JTextField();
        allowOnly(n, "[^0-9]*", 30);

        JComboBox<String> t = new JComboBox<>(
                new String[]{"PEMASUKAN", "PENGELUARAN"}
        );

        if (Dialogs.showConfirmDialog(
                this,
                new Object[]{
                        "Nama Kategori:", n,
                        "Jenis Transaksi:", t
                },
                "Tambah Kategori Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            if (n.getText().isBlank()) {
                info("Nama kategori tidak boleh kosong.");
                return;
            }
            dao.tambahKategori(
                    user.getIdPengguna(),
                    n.getText(),
                    (String) t.getSelectedItem()
            );

            info("Kategori berhasil ditambahkan.");

        } catch (Exception e) {
            error(e);
        }
    }

    // ==========================================
    // TRANSFER DOMPET - TANPA MENAMPILKAN ID
    // ==========================================

    private void transfer() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());

            if (wallets.size() < 2) {
                info("Minimal harus ada dua dompet aktif untuk melakukan transfer.");
                return;
            }

            // Nama dan saldo saja yang ditampilkan.
            // ID tetap tersimpan pada data wallets.
            List<String> items = wallets.stream()
                    .map(w -> w.nama() + " (" + money(w.saldo()) + ")")
                    .toList();

            JComboBox<String> src = combo(items, 0);
            JComboBox<String> dst = combo(items, 1);

            JTextField nominal = new JTextField();
            rupiahInput(nominal);
            JTextField ket = new JTextField();

            if (Dialogs.showConfirmDialog(
                    this,
                    new Object[]{
                            "Dompet Sumber (Dari):", src,
                            "Dompet Tujuan (Ke):", dst,
                            "Nominal Transfer (Rp):", nominal,
                            "Catatan/Keterangan:", ket
                    },
                    "Konfirmasi Transfer",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            if (src.getSelectedIndex() < 0 || dst.getSelectedIndex() < 0) {
                info("Pilih dompet sumber dan tujuan terlebih dahulu.");
                return;
            }

            // Mengambil ID asli dari daftar dompet berdasarkan pilihan.
            long sourceId = wallets.get(src.getSelectedIndex()).id();
            long destinationId = wallets.get(dst.getSelectedIndex()).id();

            if (sourceId == destinationId) {
                info("Dompet sumber dan dompet tujuan tidak boleh sama.");
                return;
            }

            BigDecimal jumlah = amount(nominal.getText());
            String catatan = ket.getText();

            dao.transfer(
                    user.getIdPengguna(),
                    sourceId,
                    destinationId,
                    jumlah,
                    catatan
            );

            String bukti =
                    "========================================\n" +
                            "           BUKTI TRANSFER DOMPETKU      \n" +
                            "========================================\n" +
                            "Tanggal  : " + LocalDate.now() + "\n" +
                            "Dari     : " + src.getSelectedItem() + "\n" +
                            "Tujuan   : " + dst.getSelectedItem() + "\n" +
                            "Nominal  : " + money(jumlah) + "\n" +
                            "Catatan  : " + catatan + "\n" +
                            "Status   : BERHASIL & TEREKAM DI RIWAYAT\n" +
                            "========================================";

            JTextArea areaBukti = new JTextArea(bukti);
            areaBukti.setEditable(false);
            areaBukti.setFont(new Font("Monospaced", Font.PLAIN, 13));

            Dialogs.showConfirmDialog(
                    this,
                    new JScrollPane(areaBukti),
                    "Transfer Berhasil - Cetak Bukti",
                    JOptionPane.OK_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel budgets() {
        JPanel p = Theme.page();

        JTable table = new JTable();
        styleTable(table);

        JButton add = Theme.button("+ Set Anggaran");
        add.addActionListener(e -> addBudget());

        p.add(
                Theme.pageHeader("Anggaran Pengeluaran Bulanan", Theme.buttonRow(add)),
                BorderLayout.NORTH
        );

        p.add(createTableContainer(table), BorderLayout.CENTER);

        loadBudgets(table);
        refreshTasks.add(() -> loadBudgets(table));

        return p;
    }

    private void loadBudgets(JTable table) {
        try {
            DefaultTableModel m = model(
                    "ID", "Kategori", "Mulai", "Selesai",
                    "Batas Anggaran", "Terpakai", "Penggunaan", "Status"
            );

            for (String[] r : dao.anggaran(user.getIdPengguna())) {
                BigDecimal limit = new BigDecimal(r[4]);
                BigDecimal used = new BigDecimal(r[5]);
                String pct = limit.signum() == 0
                        ? "-"
                        : used.multiply(new BigDecimal("100"))
                        .divide(limit, 1, java.math.RoundingMode.HALF_UP) + "%";

                m.addRow(new Object[]{
                        r[0], r[1], tgl(r[2]), tgl(r[3]),
                        money(limit), money(used), pct,
                        used.compareTo(limit) > 0 ? "Lebih" : "Aman"
                });
            }

            table.setModel(m);
            hideIdColumn(table);

        } catch (Exception e) {
            error(e);
        }
    }

    private void addBudget() {
        try {
            var cats = dao.kategori(user.getIdPengguna(), "PENGELUARAN");

            if (cats.isEmpty()) {
                info("Tambahkan kategori pengeluaran terlebih dahulu.");
                return;
            }

            JComboBox<KeuanganDAO.KategoriRow> c = kategoriCombo(cats);

            JTextField mulai = new JTextField(
                    YearMonth.now().atDay(1).toString()
            );

            JTextField akhir = new JTextField(
                    YearMonth.now().atEndOfMonth().toString()
            );

            JTextField batas = new JTextField();
            rupiahInput(batas);

            if (Dialogs.showConfirmDialog(
                    this,
                    new Object[]{
                            "Kategori Pengeluaran:", c,
                            "Tanggal Mulai (YYYY-MM-DD):", mulai,
                            "Tanggal Selesai (YYYY-MM-DD):", akhir,
                            "Batas Anggaran (Rp):", batas
                    },
                    "Tambah Anggaran Pengeluaran",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            KeuanganDAO.KategoriRow selectedKategori =
                    (KeuanganDAO.KategoriRow) c.getSelectedItem();

            if (selectedKategori == null) {
                info("Pilih kategori pengeluaran terlebih dahulu.");
                return;
            }

            dao.tambahAnggaran(
                    user.getIdPengguna(),
                    selectedKategori.id(),
                    LocalDate.parse(mulai.getText()),
                    LocalDate.parse(akhir.getText()),
                    amount(batas.getText())
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel savings() {
        JPanel p = Theme.page();

        JTable table = new JTable();
        styleTable(table);

        JButton add = Theme.button("+ Target Baru");
        JButton con = Theme.buttonDark("+ Tambah Setoran");

        add.addActionListener(e -> addTarget());
        con.addActionListener(e -> contribute());

        p.add(
                Theme.pageHeader("Target Tabungan & Impian", Theme.buttonRow(con, add)),
                BorderLayout.NORTH
        );

        p.add(createTableContainer(table), BorderLayout.CENTER);

        loadTargets(table);
        refreshTasks.add(() -> loadTargets(table));

        return p;
    }

    private void loadTargets(JTable table) {
        try {
            DefaultTableModel m = model(
                    "ID", "Nama Target", "Target Nominal", "Terkumpul",
                    "Status", "Tenggat", "Estimasi Tercapai", "Rekomendasi Nabung"
            );

            Locale idn = Locale.forLanguageTag("id-ID");
            DateTimeFormatter bulanFmt = DateTimeFormatter.ofPattern("MMMM yyyy", idn);

            for (String[] r : dao.target(user.getIdPengguna())) {
                BigDecimal target = new BigDecimal(r[2]);
                BigDecimal kumpul = new BigDecimal(r[3]);
                BigDecimal sisa = target.subtract(kumpul);
                LocalDate tenggat = r[5] == null ? null : LocalDate.parse(r[5]);
                LocalDate setoranPertama = r[6] == null ? null : LocalDate.parse(r[6]);

                String estimasi;
                String rekomendasi;

                if ("TERCAPAI".equalsIgnoreCase(r[4]) || sisa.signum() <= 0) {
                    estimasi = "Sudah tercapai";
                    rekomendasi = "-";

                } else {
                    // ---- Estimasi: rata-rata setoran per bulan sejak setoran pertama
                    if (kumpul.signum() <= 0 || setoranPertama == null) {
                        estimasi = "Belum ada setoran";
                    } else {
                        long bulanBerjalan = java.time.temporal.ChronoUnit.MONTHS.between(
                                YearMonth.from(setoranPertama), YearMonth.now()) + 1;
                        BigDecimal rataPerBulan = kumpul.divide(
                                BigDecimal.valueOf(bulanBerjalan), 2, java.math.RoundingMode.HALF_UP);
                        long bulanLagi = sisa.divide(
                                rataPerBulan, 0, java.math.RoundingMode.CEILING).longValue();

                        YearMonth perkiraan = YearMonth.now().plusMonths(bulanLagi);
                        estimasi = perkiraan.format(bulanFmt);

                        if (tenggat != null && perkiraan.isAfter(YearMonth.from(tenggat))) {
                            estimasi += " (lewat tenggat)";
                        }
                    }

                    // ---- Rekomendasi nabung per hari / per bulan sampai tenggat
                    if (tenggat == null) {
                        rekomendasi = "-";
                    } else {
                        long hari = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), tenggat);

                        if (hari <= 0) {
                            rekomendasi = "Tenggat terlewati, sisa " + money(sisa);
                        } else {
                            long bulan = Math.max(1, (long) Math.ceil(hari / 30.0));
                            BigDecimal perHari = sisa.divide(
                                    BigDecimal.valueOf(hari), 0, java.math.RoundingMode.CEILING);
                            BigDecimal perBulan = sisa.divide(
                                    BigDecimal.valueOf(bulan), 0, java.math.RoundingMode.CEILING);
                            rekomendasi = money(perHari) + "/hari atau " + money(perBulan) + "/bulan";
                        }
                    }
                }

                m.addRow(new Object[]{
                        r[0],
                        r[1],
                        money(target),
                        money(kumpul),
                        r[4],
                        tgl(r[5]),
                        estimasi,
                        rekomendasi
                });
            }

            table.setModel(m);
            hideIdColumn(table);
            setColumnWidths(table, 150, 120, 120, 90, 120, 170, 280);

        } catch (Exception e) {
            error(e);
        }
    }

    private void addTarget() {
        JTextField n = new JTextField();
        JTextField v = new JTextField();
        rupiahInput(v);

        JTextField d = new JTextField(
                LocalDate.now().plusMonths(6).toString()
        );

        if (Dialogs.showConfirmDialog(
                this,
                new Object[]{
                        "Nama Impian/Target:", n,
                        "Target Nominal (Rp):", v,
                        "Tenggat Tanggal (YYYY-MM-DD):", d
                },
                "Buat Target Tabungan Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            dao.tambahTarget(
                    user.getIdPengguna(),
                    n.getText(),
                    amount(v.getText()),
                    LocalDate.parse(d.getText())
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    // ==========================================
    // SETORAN TABUNGAN - TANPA MENAMPILKAN ID
    // ==========================================

    private void contribute() {
        try {
            var targets = dao.target(user.getIdPengguna());

            if (targets.isEmpty()) {
                info("Buat target tabungan terlebih dahulu.");
                return;
            }

            JComboBox<String> c = combo(
                    targets.stream()
                            .map(r -> r[1] + " (" + r[4] + ")")
                            .toList()
            );

            JTextField n = new JTextField();
            rupiahInput(n);
            JTextField k = new JTextField();

            if (Dialogs.showConfirmDialog(
                    this,
                    new Object[]{
                            "Target Impian:", c,
                            "Nominal Setoran (Rp):", n,
                            "Keterangan/Sumber Dana:", k
                    },
                    "Tambah Setoran Tabungan",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            if (c.getSelectedIndex() < 0) {
                info("Pilih target tabungan terlebih dahulu.");
                return;
            }

            // ID asli diambil dari list target berdasarkan urutan pilihan
            long idTarget = Long.parseLong(targets.get(c.getSelectedIndex())[0]);

            dao.kontribusi(
                    user.getIdPengguna(),
                    idTarget,
                    amount(n.getText()),
                    k.getText()
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel debts() {
        JPanel p = Theme.page();

        JTable table = new JTable();
        styleTable(table);

        JButton add = Theme.button("+ Catat Utang/Piutang");
        JButton pay = Theme.buttonDark("+ Bayar / Cicil");

        add.addActionListener(e -> addDebt());
        pay.addActionListener(e -> payDebt());

        p.add(
                Theme.pageHeader("Catatan Utang & Piutang", Theme.buttonRow(pay, add)),
                BorderLayout.NORTH
        );

        p.add(createTableContainer(table), BorderLayout.CENTER);

        loadDebts(table);
        refreshTasks.add(() -> loadDebts(table));

        return p;
    }

    private void loadDebts(JTable table) {
        try {
            DefaultTableModel m = model(
                    "ID", "Jenis", "Nama Pihak", "Nominal Awal",
                    "Sudah Dibayar", "Jatuh Tempo", "Status"
            );

            for (String[] r : dao.utang(user.getIdPengguna())) {
                m.addRow(new Object[]{
                        r[0],
                        r[1],
                        r[2],
                        money(new BigDecimal(r[3])),
                        money(new BigDecimal(r[4])),
                        (tgl(r[5])),
                        r[6]
                });
            }

            table.setModel(m);
            hideIdColumn(table);

        } catch (Exception e) {
            error(e);
        }
    }

    private void addDebt() {
        JComboBox<String> jenis = new JComboBox<>(
                new String[]{"UTANG", "PIUTANG"}
        );

        JTextField pihak = new JTextField();
        JTextField nom = new JTextField();

        JTextField tempo = new JTextField(
                LocalDate.now().plusMonths(1).toString()
        );

        if (Dialogs.showConfirmDialog(
                this,
                new Object[]{
                        "Jenis Catatan:", jenis,
                        "Nama Pihak (Orang/Lembaga):", pihak,
                        "Nominal Utama (Rp):", nom,
                        "Jatuh Tempo (YYYY-MM-DD):", tempo
                },
                "Catat Utang/Piutang Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            dao.tambahUtang(
                    user.getIdPengguna(),
                    (String) jenis.getSelectedItem(),
                    pihak.getText(),
                    amount(nom.getText()),
                    LocalDate.parse(tempo.getText())
            );

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    // ==========================================
    // BAYAR / CICIL UTANG - TANPA MENAMPILKAN ID
    // ==========================================

    private void payDebt() {
        try {
            var rows = dao.utang(user.getIdPengguna());

            if (rows.isEmpty()) {
                info("Belum ada catatan utang/piutang.");
                return;
            }

            var wallets = dao.dompet(user.getIdPengguna());

            JComboBox<String> c = combo(
                    rows.stream()
                            .map(r -> r[1] + " - " + r[2] + " (" + r[6] + ")")
                            .toList()
            );

            // Indeks 0 = tanpa dompet, indeks 1 dst = wallets.get(indeks - 1)
            List<String> opsiDompet = new ArrayList<>();
            opsiDompet.add("(Tanpa dompet, hanya catat pembayaran)");
            wallets.forEach(w -> opsiDompet.add(
                    w.nama() + " (" + money(w.saldo()) + ")"));

            JComboBox<String> walletCombo = combo(opsiDompet);

            JTextField n = new JTextField();
            rupiahInput(n);

            if (Dialogs.showConfirmDialog(
                    this,
                    new Object[]{
                            "Pilih Catatan Utang/Piutang:", c,
                            "Dompet :", walletCombo,
                            "Nominal Pembayaran (Rp):", n
                    },
                    "Catat Pembayaran / Angsuran",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            if (c.getSelectedIndex() < 0) {
                info("Pilih catatan utang/piutang terlebih dahulu.");
                return;
            }

            // ID asli diambil dari list berdasarkan urutan pilihan
            long idUtang = Long.parseLong(rows.get(c.getSelectedIndex())[0]);

            int walletIndex = walletCombo.getSelectedIndex();
            Long idDompet = walletIndex <= 0
                    ? null
                    : wallets.get(walletIndex - 1).id();

            BigDecimal nominalBayar = amount(n.getText());

            dao.bayarUtang(
                    user.getIdPengguna(),
                    idUtang,
                    nominalBayar,
                    idDompet
            );

            info(idDompet == null
                    ? "Pembayaran dicatat tanpa mengubah saldo dompet."
                    : "Pembayaran dicatat dan saldo dompet diperbarui.");

            refreshAll();

        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel reports() {
        JPanel p = Theme.page();

        JTextField start = new JTextField(
                YearMonth.now().atDay(1).toString(), 14
        );

        JTextField end = new JTextField(
                YearMonth.now().atEndOfMonth().toString(), 14
        );

        start.setFont(new Font("SansSerif", Font.PLAIN, 15));
        end.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JTextArea out = new JTextArea();
        out.setEditable(false);
        out.setFont(new Font("Monospaced", Font.PLAIN, 13));
        out.setMargin(new Insets(16, 16, 16, 16));

        CategoryPiePanel incomeChart =
                new CategoryPiePanel("RINCIAN PEMASUKAN");

        CategoryPiePanel expenseChart =
                new CategoryPiePanel("RINCIAN PENGELUARAN");

        JButton show = Theme.button("Tampilkan Laporan");
        JButton csv = Theme.button("Ekspor File CSV");

        show.addActionListener(e -> {
            try {
                LocalDate mulai = LocalDate.parse(start.getText());
                LocalDate akhir = LocalDate.parse(end.getText());

                var ringkasan = dao.ringkasan(
                        user.getIdPengguna(), mulai, akhir
                );

                var kategoriPemasukan = dao.rincianKategori(
                        user.getIdPengguna(),
                        "PEMASUKAN",
                        mulai,
                        akhir
                );

                var kategoriPengeluaran = dao.rincianKategori(
                        user.getIdPengguna(),
                        "PENGELUARAN",
                        mulai,
                        akhir
                );

                incomeChart.setData(kategoriPemasukan);
                expenseChart.setData(kategoriPengeluaran);

                var tren = dao.trenBulanan(
                        user.getIdPengguna(),
                        mulai,
                        akhir
                );

                StringBuilder laporan = new StringBuilder();

                laporan.append("====================================================\n");
                laporan.append("               LAPORAN KEUANGAN DOMPETKU             \n");
                laporan.append("====================================================\n");

                laporan.append("Periode : ")
                        .append(mulai)
                        .append(" s.d. ")
                        .append(akhir)
                        .append("\n\n");

                laporan.append("RINGKASAN KEUANGAN\n");
                laporan.append("----------------------------------------------------\n");

                laporan.append("Total Pemasukan   : ")
                        .append(money(ringkasan.pemasukan()))
                        .append("\n");

                laporan.append("Total Pengeluaran : ")
                        .append(money(ringkasan.pengeluaran()))
                        .append("\n");

                laporan.append("Selisih Bersih    : ")
                        .append(money(ringkasan.selisih()))
                        .append("\n\n");

                laporan.append("\nRINCIAN PEMASUKAN BERDASARKAN KATEGORI\n");
                laporan.append("----------------------------------------------------\n");

                if (kategoriPemasukan.isEmpty()) {
                    laporan.append("Belum ada data pemasukan.\n");
                } else {
                    for (var item : kategoriPemasukan) {
                        laporan.append(item.nama())
                                .append(" : ")
                                .append(money(item.total()))
                                .append(" (")
                                .append(item.persentase().toPlainString())
                                .append("%)\n");
                    }
                }

                laporan.append("\nRINCIAN PENGELUARAN BERDASARKAN KATEGORI\n");
                laporan.append("----------------------------------------------------\n");

                if (kategoriPengeluaran.isEmpty()) {
                    laporan.append("Belum ada data pengeluaran.\n");
                } else {
                    for (var item : kategoriPengeluaran) {
                        laporan.append(item.nama())
                                .append(" : ")
                                .append(money(item.total()))
                                .append(" (")
                                .append(item.persentase().toPlainString())
                                .append("%)\n");
                    }
                }

                laporan.append("\nTREN KEUANGAN BULANAN\n");
                laporan.append("----------------------------------------------------\n");

                if (tren.isEmpty()) {
                    laporan.append("Belum ada data tren keuangan.\n");
                } else {
                    for (var item : tren) {
                        laporan.append(item.bulan())
                                .append("\n");

                        laporan.append("  Pemasukan   : ")
                                .append(money(item.pemasukan()))
                                .append("\n");

                        laporan.append("  Pengeluaran : ")
                                .append(money(item.pengeluaran()))
                                .append("\n\n");
                    }
                }

                laporan.append("====================================================\n");

                out.setText(laporan.toString());
                out.setCaretPosition(0);

            } catch (Exception ex) {
                error(ex);
            }
        });

        csv.addActionListener(e -> {
            try {
                LocalDate mulai = LocalDate.parse(start.getText());
                LocalDate akhir = LocalDate.parse(end.getText());

                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(
                        new java.io.File("laporan-dompetku.csv")
                );

                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    dao.eksporCsv(
                            user.getIdPengguna(),
                            fc.getSelectedFile().toPath(),
                            mulai,
                            akhir
                    );

                    info("Laporan CSV berhasil diekspor.");
                }

            } catch (Exception ex) {
                error(ex);
            }
        });

        JLabel lblStart = new JLabel("Mulai:");
        lblStart.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel lblEnd = new JLabel("Selesai:");
        lblEnd.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel filterBar = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 0)
        );

        filterBar.setOpaque(false);
        filterBar.add(lblStart);
        filterBar.add(start);
        filterBar.add(lblEnd);
        filterBar.add(end);
        filterBar.add(show);
        filterBar.add(csv);

        JPanel reportWrapper = new JPanel(new BorderLayout(0, 10));
        reportWrapper.setOpaque(false);
        reportWrapper.add(filterBar, BorderLayout.NORTH);

        // Panel untuk menampung laporan teks dan grafik
        JPanel chartContainer = new JPanel(
                new GridLayout(1, 2, 16, 0)
        );

        chartContainer.setBackground(Theme.WHITE);
        chartContainer.setBorder(
                new EmptyBorder(0, 0, 5, 0)
        );

        chartContainer.add(incomeChart);
        chartContainer.add(expenseChart);

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(
                new BoxLayout(resultPanel, BoxLayout.Y_AXIS)
        );
        resultPanel.setBackground(Theme.WHITE);

        out.setRows(18);
        out.setColumns(60);

        JScrollPane textScroll = new JScrollPane(out);
        textScroll.setPreferredSize(new Dimension(900, 400));

        resultPanel.add(textScroll);
        resultPanel.add(Box.createVerticalStrut(16));
        resultPanel.add(chartContainer);

        JScrollPane scroll = new JScrollPane(resultPanel);
        scroll.setBorder(
                BorderFactory.createLineBorder(Theme.BORDER)
        );

        reportWrapper.add(scroll, BorderLayout.CENTER);

        p.add(
                Theme.heading("Laporan Keuangan & Analisis"),
                BorderLayout.NORTH
        );

        p.add(reportWrapper, BorderLayout.CENTER);

        return p;
    }

    private static class CategoryPiePanel extends JPanel {

        private static final NumberFormat RP =
                NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));

        static {
            RP.setMinimumFractionDigits(0);
            RP.setMaximumFractionDigits(0);
        }

        private static final Color[] PALETTE = {
                new Color(91, 108, 214),   // Indigo
                new Color(32, 163, 158),   // Teal
                new Color(238, 108, 88),   // Coral
                new Color(150, 104, 205),  // Violet
                new Color(62, 170, 110),   // Emerald
                new Color(70, 160, 225),   // Biru langit
                new Color(222, 92, 140),   // Rose
                new Color(100, 120, 145),  // Slate
                new Color(110, 190, 170),  // Mint
                new Color(120, 80, 130)    // Plum
        };

        private static final int RING_TOP = 72;
        private static final int MAX_DIAMETER = 220;
        private static final int ROW_H = 40;
        private static final double INNER_RATIO = 0.60;

        private final String title;

        private List<KeuanganDAO.KategoriLaporan> data = new ArrayList<>();
        private BigDecimal total = BigDecimal.ZERO;

        private double progress = 1.0;
        private int hover = -1;

        private final List<Rectangle> legendRows = new ArrayList<>();
        private javax.swing.Timer animator;

        public CategoryPiePanel(String title) {
            this.title = title;

            setBackground(Color.WHITE);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    new EmptyBorder(15, 12, 15, 12)
            ));

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateHover(e.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHover(-1);
                }
            };

            addMouseMotionListener(mouse);
            addMouseListener(mouse);
        }

        public void setData(List<KeuanganDAO.KategoriLaporan> newData) {
            data = new ArrayList<>(newData);

            total = BigDecimal.ZERO;
            for (var item : data) {
                total = total.add(item.total());
            }

            hover = -1;
            revalidate();
            startAnimation();
        }

        // ---------------------------------------------------------
        // ANIMASI: cincin "tumbuh" dari atas searah jarum jam
        // ---------------------------------------------------------

        private void startAnimation() {
            if (animator != null) {
                animator.stop();
            }

            if (data.isEmpty()) {
                progress = 1;
                repaint();
                return;
            }

            progress = 0;
            final long begin = System.nanoTime();

            animator = new javax.swing.Timer(15, e -> {
                double t = (System.nanoTime() - begin) / 650_000_000.0;

                if (t >= 1) {
                    t = 1;
                    ((javax.swing.Timer) e.getSource()).stop();
                }

                progress = 1 - Math.pow(1 - t, 3); // ease-out
                repaint();
            });

            animator.start();
        }

        @Override
        public Dimension getPreferredSize() {
            int legendTop = RING_TOP + MAX_DIAMETER + 30;
            return new Dimension(430, legendTop + data.size() * ROW_H + 24);
        }

        // ---------------------------------------------------------
        // HOVER: arahkan mouse ke irisan atau baris legenda
        // ---------------------------------------------------------

        private void setHover(int index) {
            if (index != hover) {
                hover = index;
                setCursor(index >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                repaint();
            }
        }

        private void updateHover(Point p) {
            int found = -1;

            for (int i = 0; i < legendRows.size(); i++) {
                if (legendRows.get(i).contains(p)) {
                    found = i;
                    break;
                }
            }

            if (found < 0 && !data.isEmpty() && total.signum() > 0) {
                double d = diameter();
                double cx = getWidth() / 2.0;
                double cy = RING_TOP + d / 2.0;

                double dx = p.x - cx;
                double dy = p.y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);

                double outer = d / 2.0;
                double inner = outer * INNER_RATIO;

                if (dist >= inner && dist <= outer + 8) {
                    // sudut searah jarum jam dari posisi jam 12
                    double angle = Math.toDegrees(Math.atan2(dx, -dy));
                    if (angle < 0) angle += 360;

                    double acc = 0;

                    for (int i = 0; i < data.size(); i++) {
                        double sweep = share(i) * 360;

                        if (angle >= acc && angle < acc + sweep) {
                            found = i;
                            break;
                        }

                        acc += sweep;
                    }
                }
            }

            setHover(found);
        }

        // ---------------------------------------------------------
        // UTILITAS
        // ---------------------------------------------------------

        private int diameter() {
            return Math.max(120, Math.min(MAX_DIAMETER, getWidth() - 90));
        }

        private double share(int i) {
            return data.get(i).total().doubleValue() / total.doubleValue();
        }

        private Color colorOf(int i) {
            return PALETTE[i % PALETTE.length];
        }

        private static Color mix(Color a, Color b, double t) {
            return new Color(
                    (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t)
            );
        }

        private static String ellipsize(String s, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(s) <= maxWidth) return s;

            String t = s;

            while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) {
                t = t.substring(0, t.length() - 1);
            }

            return t + "…";
        }

        private static Font fitFont(Graphics2D g2, String text, int maxWidth,
                                    int startSize, int style) {
            int size = startSize;
            Font f = new Font("SansSerif", style, size);

            while (size > 9 && g2.getFontMetrics(f).stringWidth(text) > maxWidth) {
                size--;
                f = new Font("SansSerif", style, size);
            }

            return f;
        }

        private static void drawCentered(Graphics2D g2, String text,
                                         double cx, double baselineY) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0),
                    (float) baselineY);
        }

        // ---------------------------------------------------------
        // PENGGAMBARAN
        // ---------------------------------------------------------

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();

            drawTitle(g2, w);

            legendRows.clear();

            if (data.isEmpty() || total.signum() <= 0) {
                drawEmpty(g2, w);
                g2.dispose();
                return;
            }

            drawDonut(g2, w);
            drawCenterText(g2, w);
            drawLegend(g2, w);

            g2.dispose();
        }

        private void drawTitle(Graphics2D g2, int w) {
            g2.setColor(Theme.TEXT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 17));

            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, (w - fm.stringWidth(title)) / 2, 30);

            g2.setColor(new Color(225, 220, 205));
            g2.drawLine(20, 45, w - 20, 45);
        }

        private void drawEmpty(Graphics2D g2, int w) {
            double d = diameter();
            double x0 = (w - d) / 2.0;
            double y0 = RING_TOP;
            double hole = d * INNER_RATIO;
            double hx = x0 + (d - hole) / 2.0;
            double hy = y0 + (d - hole) / 2.0;

            Area ring = new Area(new Ellipse2D.Double(x0, y0, d, d));
            ring.subtract(new Area(new Ellipse2D.Double(hx, hy, hole, hole)));

            g2.setColor(new Color(238, 234, 225));
            g2.fill(ring);

            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

            double cy = y0 + d / 2.0;
            drawCentered(g2, "Belum ada transaksi", w / 2.0, cy - 2);
            drawCentered(g2, "pada periode ini", w / 2.0, cy + 15);
        }

        private void drawDonut(Graphics2D g2, int w) {
            double d = diameter();
            double x0 = (w - d) / 2.0;
            double y0 = RING_TOP;

            double hole = d * INNER_RATIO;
            double hx = x0 + (d - hole) / 2.0;
            double hy = y0 + (d - hole) / 2.0;

            Ellipse2D holeShape = new Ellipse2D.Double(hx, hy, hole, hole);

            // ---- bayangan lembut di bawah cincin (memberi kesan kedalaman)
            for (int i = 8; i >= 1; i--) {
                double grow = i * 1.5;

                g2.setColor(new Color(60, 45, 20, 7));
                g2.fill(new Ellipse2D.Double(
                        x0 - grow, y0 + 9 - grow, d + 2 * grow, d + 2 * grow
                ));
            }

            // ---- irisan-irisan cincin
            int n = data.size();
            double gap = n > 1 ? 2.5 : 0;
            double start = 90; // jam 12, bergerak searah jarum jam

            for (int i = 0; i < n; i++) {
                double sweep = share(i) * 360 * progress;
                double extent = Math.max(sweep - gap, sweep * 0.5);

                if (extent <= 0.01) {
                    start -= sweep;
                    continue;
                }

                // satu kategori penuh (360 derajat) digambar sebagai lingkaran utuh
                // agar tidak ada garis sambungan di posisi jam 12
                Area seg = extent >= 359.9
                        ? new Area(new Ellipse2D.Double(x0, y0, d, d))
                        : new Area(new Arc2D.Double(
                        x0, y0, d, d,
                        start - (sweep - extent) / 2.0,
                        -extent,
                        Arc2D.PIE
                ));

                seg.subtract(new Area(holeShape));

                // irisan yang disorot "menonjol" keluar
                if (i == hover) {
                    double mid = Math.toRadians(start - sweep / 2.0);
                    seg.transform(AffineTransform.getTranslateInstance(
                            Math.cos(mid) * 7, -Math.sin(mid) * 7
                    ));
                }

                Color base = colorOf(i);

                if (hover >= 0 && hover != i) {
                    base = mix(base, Color.WHITE, 0.45); // yang lain meredup
                }

                Color light = mix(base, Color.WHITE, 0.22);
                Color dark = mix(base, Color.BLACK, 0.12);

                g2.setPaint(new GradientPaint(
                        0f, (float) y0, light,
                        0f, (float) (y0 + d), dark
                ));
                g2.fill(seg);

                // tepi tipis terang agar terlihat berlapis
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(255, 255, 255, 90));
                g2.draw(seg);

                start -= sweep;
            }

            // ---- lubang tengah dengan bayangan dalam
            g2.setColor(Color.WHITE);
            g2.fill(holeShape);

            g2.setPaint(new RadialGradientPaint(
                    new Point2D.Double(w / 2.0, y0 + d / 2.0),
                    (float) (hole / 2.0),
                    new float[]{0f, 0.82f, 1f},
                    new Color[]{
                            new Color(255, 255, 255, 0),
                            new Color(255, 255, 255, 0),
                            new Color(60, 45, 20, 38)
                    }
            ));
            g2.fill(holeShape);

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0, 0, 0, 18));
            g2.draw(holeShape);
        }

        private void drawCenterText(Graphics2D g2, int w) {
            double d = diameter();
            double cx = w / 2.0;
            double cy = RING_TOP + d / 2.0;
            int maxWidth = (int) (d * INNER_RATIO * 0.80);

            String line1;
            String line2;
            String line3;
            Color color1;

            if (hover >= 0 && hover < data.size()) {
                var item = data.get(hover);
                line1 = item.nama();
                line2 = RP.format(item.total());
                line3 = item.persentase().toPlainString() + "%";
                color1 = colorOf(hover);
            } else {
                line1 = "TOTAL";
                line2 = RP.format(total);
                line3 = data.size() + " kategori";
                color1 = Color.GRAY;
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(color1);
            drawCentered(g2, ellipsize(line1, g2.getFontMetrics(), maxWidth),
                    cx, cy - 14);

            g2.setFont(fitFont(g2, line2, maxWidth, 17, Font.BOLD));
            g2.setColor(Theme.TEXT);
            drawCentered(g2, line2, cx, cy + 8);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(Color.GRAY);
            drawCentered(g2, line3, cx, cy + 26);
        }

        private void drawLegend(Graphics2D g2, int w) {
            int top = (int) (RING_TOP + diameter() + 30);
            int padX = 16;
            int right = w - padX;

            Font nameFont = new Font("SansSerif", Font.BOLD, 12);
            Font pctFont = new Font("SansSerif", Font.PLAIN, 12);
            Font amtFont = new Font("SansSerif", Font.BOLD, 12);

            FontMetrics amtFM = g2.getFontMetrics(amtFont);
            FontMetrics pctFM = g2.getFontMetrics(pctFont);
            FontMetrics nameFM = g2.getFontMetrics(nameFont);

            int amtW = 0;
            int pctW = 0;

            for (var item : data) {
                amtW = Math.max(amtW, amtFM.stringWidth(RP.format(item.total())));
                pctW = Math.max(pctW,
                        pctFM.stringWidth(item.persentase().toPlainString() + "%"));
            }

            int amtRight = right;
            int pctRight = amtRight - amtW - 14;
            int nameX = 8 + 46;
            int nameMax = pctRight - pctW - 12 - nameX;

            for (int i = 0; i < data.size(); i++) {
                var item = data.get(i);

                Rectangle row = new Rectangle(8, top + i * ROW_H, w - 16, ROW_H - 2);
                legendRows.add(row);

                if (i == hover) {
                    g2.setColor(new Color(248, 244, 234));
                    g2.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
                }

                int midY = row.y + row.height / 2;
                Color c = colorOf(i);

                // lingkaran ikon lembut + titik warna (mirip daftar kategori e-wallet)
                g2.setColor(mix(c, Color.WHITE, 0.80));
                g2.fillOval(row.x + 10, midY - 13, 26, 26);

                g2.setColor(c);
                g2.fillOval(row.x + 18, midY - 5, 10, 10);

                // nama kategori
                g2.setFont(nameFont);
                g2.setColor(Theme.TEXT);
                g2.drawString(
                        ellipsize(item.nama(), nameFM, nameMax),
                        nameX,
                        midY + (nameFM.getAscent() - nameFM.getDescent()) / 2
                );

                // persentase
                String pct = item.persentase().toPlainString() + "%";
                g2.setFont(pctFont);
                g2.setColor(Color.GRAY);
                g2.drawString(
                        pct,
                        pctRight - pctFM.stringWidth(pct),
                        midY + (pctFM.getAscent() - pctFM.getDescent()) / 2
                );

                // nominal
                String amt = RP.format(item.total());
                g2.setFont(amtFont);
                g2.setColor(Theme.TEXT);
                g2.drawString(
                        amt,
                        amtRight - amtFM.stringWidth(amt),
                        midY + (amtFM.getAscent() - amtFM.getDescent()) / 2
                );

                // garis pemisah antar baris
                if (i < data.size() - 1) {
                    g2.setColor(new Color(240, 236, 226));
                    g2.drawLine(nameX, row.y + ROW_H - 1, right, row.y + ROW_H - 1);
                }
            }
        }
    }

    private JScrollPane createTableContainer(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        return scroll;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setRowHeight(38);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.WHITE);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();

        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(Theme.CHARCOAL);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 40));

        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl,
                    Object value,
                    boolean selected,
                    boolean focus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, selected, focus, row, column
                );

                label.setOpaque(true);
                label.setBackground(Theme.CHARCOAL);
                label.setForeground(Color.WHITE);
                label.setFont(new Font("SansSerif", Font.BOLD, 13));

                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0, 0, 2, 0, Theme.GOLD
                        ),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));

                label.setHorizontalAlignment(SwingConstants.LEFT);

                return label;
            }
        });

        table.setDefaultRenderer(Object.class,
                new javax.swing.table.DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tbl,
                            Object value,
                            boolean selected,
                            boolean focus,
                            int row,
                            int column) {

                        JLabel label = (JLabel) super.getTableCellRendererComponent(
                                tbl, value, selected, focus, row, column
                        );

                        label.setFont(new Font("SansSerif", Font.PLAIN, 14));

                        label.setBorder(
                                BorderFactory.createEmptyBorder(4, 12, 4, 12)
                        );

                        label.setToolTipText(
                                value == null ? null : value.toString()
                        );

                        if (selected) {
                            label.setBackground(Theme.SELECTION);
                            label.setForeground(Theme.TEXT);
                            return label;
                        }

                        label.setBackground(Theme.WHITE);
                        label.setForeground(Theme.TEXT);

                        int jenisColumn = -1;

                        for (int i = 0; i < tbl.getColumnCount(); i++) {
                            if ("Jenis".equalsIgnoreCase(tbl.getColumnName(i))) {
                                jenisColumn = i;
                                break;
                            }
                        }

                        if (jenisColumn >= 0) {
                            Object jenis = tbl.getValueAt(row, jenisColumn);

                            if (jenis != null) {
                                String tipe = jenis.toString()
                                        .toUpperCase(Locale.ROOT);

                                if (tipe.contains("PEMASUKAN")) {
                                    label.setForeground(Theme.INCOME);
                                    label.setFont(
                                            new Font("SansSerif", Font.BOLD, 14)
                                    );

                                } else if (tipe.contains("PENGELUARAN")) {
                                    label.setForeground(Theme.EXPENSE);
                                    label.setFont(
                                            new Font("SansSerif", Font.BOLD, 14)
                                    );
                                }
                            }
                        }

                        String v = value == null ? "" : value.toString();
                        if ("Lebih".equals(v)) {
                            label.setForeground(Theme.EXPENSE);
                            label.setFont(new Font("SansSerif", Font.BOLD, 14));
                        } else if ("Aman".equals(v)) {
                            label.setForeground(Theme.INCOME);
                            label.setFont(new Font("SansSerif", Font.BOLD, 14));
                        }

                        return label;
                    }
                });
    }

    private void refreshAll() {
        loadSummary();

        for (Runnable task : refreshTasks) {
            task.run();
        }

        revalidate();
        repaint();
    }

    private String money(BigDecimal n) {
        return rupiah.format(n == null ? BigDecimal.ZERO : n);
    }

    private BigDecimal parseNumber(String s) {
        return new BigDecimal(
                s.trim()
                        .replace("Rp", "")
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", ".")
        );
    }

    private BigDecimal amount(String s) {
        BigDecimal b = parseNumber(s);

        if (b.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Nominal harus lebih dari nol."
            );
        }

        return b;
    }

    private BigDecimal amountOrZero(String s) {
        if (s == null || s.isBlank()) {
            return BigDecimal.ZERO;
        }

        BigDecimal b = parseNumber(s);

        if (b.signum() < 0) {
            throw new IllegalArgumentException(
                    "Saldo awal tidak boleh negatif."
            );
        }

        return b;
    }

    private String plain(BigDecimal n) {
        if (n == null || n.signum() == 0) {
            return "0";
        }

        return n.stripTrailingZeros()
                .toPlainString()
                .replace('.', ',');
    }

    private String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length
                && i < table.getColumnCount(); i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(widths[i]);
        }
    }

    private void hideIdColumn(JTable table) {
        if (table.getColumnCount() > 0
                && "ID".equals(table.getColumnName(0))) {

            table.removeColumn(
                    table.getColumnModel().getColumn(0)
            );
        }
    }

    private Long selectedId(JTable table) {
        int row = table.getSelectedRow();

        if (row < 0) {
            return null;
        }

        return Long.parseLong(
                table.getModel()
                        .getValueAt(table.convertRowIndexToModel(row), 0)
                        .toString()
        );
    }

    DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    private JComboBox<KeuanganDAO.KategoriRow> kategoriCombo() {
        return kategoriCombo(List.of());
    }

    private JComboBox<KeuanganDAO.KategoriRow> kategoriCombo(
            List<KeuanganDAO.KategoriRow> categories) {

        JComboBox<KeuanganDAO.KategoriRow> combo = new JComboBox<>();

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof KeuanganDAO.KategoriRow kategori) {
                    setText(kategori.nama());
                } else {
                    setText("");
                }

                return this;
            }
        });

        for (KeuanganDAO.KategoriRow category : categories) {
            combo.addItem(category);
        }

        return combo;
    }

    private JComboBox<String> combo(List<String> items) {
        return combo(items, 0);
    }

    private JComboBox<String> combo(List<String> items, int index) {
        JComboBox<String> c = new JComboBox<>(
                items.toArray(String[]::new)
        );

        if (!items.isEmpty()) {
            c.setSelectedIndex(
                    Math.max(0, Math.min(index, items.size() - 1))
            );
        }

        return c;
    }

    // ===== HELPER REVISI =====

    // Rev 2: input nominal otomatis berformat 1.500.000 saat diketik
    private void rupiahInput(JTextField f) {
        String awal = f.getText();
        int koma = awal.indexOf(',');
        if (koma >= 0) awal = awal.substring(0, koma);
        f.setText(formatRibuan(awal.replaceAll("\\D", "")));

        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                    throws BadLocationException {
                replace(fb, off, 0, s, a);
            }

            @Override
            public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a)
                    throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, off) + (s == null ? "" : s) + cur.substring(off + len);
                String digits = next.replaceAll("\\D", "");
                if (digits.length() > 15) return;
                fb.replace(0, cur.length(), formatRibuan(digits), a);
            }

            @Override
            public void remove(FilterBypass fb, int off, int len) throws BadLocationException {
                replace(fb, off, len, "", null);
            }
        });
    }

    private String formatRibuan(String digits) {
        if (digits.isEmpty()) return "";
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID"))
                .format(new java.math.BigInteger(digits));
    }

    // Rev 4 & 5: karakter yang tidak sesuai regex tidak bisa diketik / di-paste
    private void allowOnly(JTextField f, String regex, int maxLen) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                    throws BadLocationException {
                replace(fb, off, 0, s, a);
            }

            @Override
            public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a)
                    throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, off) + (s == null ? "" : s) + cur.substring(off + len);
                if (next.length() <= maxLen && next.matches(regex)) {
                    super.replace(fb, off, len, s, a);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }

    // Rev 3: "2026-12-25" -> "25 Desember 2026"
    private String tgl(String iso) {
        if (iso == null || iso.isBlank()) return "-";
        try {
            return LocalDate.parse(iso.substring(0, Math.min(10, iso.length())))
                    .format(dateFormatter);
        } catch (Exception e) {
            return iso;
        }
    }

    private void error(Exception e) {
        String msg;

        if (e instanceof SQLException se
                && "23505".equals(se.getSQLState())) {

            msg = "Data dengan nama tersebut sudah ada. Gunakan nama lain.";

        } else if (e instanceof NumberFormatException) {

            msg = "Format angka tidak valid. Contoh: 150000";

        } else if (e instanceof java.time.format.DateTimeParseException) {

            msg = "Format tanggal tidak valid. Gunakan YYYY-MM-DD, contoh: 2026-11-30";

        } else {

            msg = e.getMessage() == null ? e.toString() : e.getMessage();
        }

        Dialogs.showMessageDialog(
                this,
                msg,
                "Kesalahan",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void info(String s) {
        Dialogs.showMessageDialog(this, s);
    }
}