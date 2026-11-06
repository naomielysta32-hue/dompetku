package id.dompetku.ui;

import id.dompetku.dao.KeuanganDAO;
import id.dompetku.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardFrame extends JFrame {

    private final Pengguna user;
    private final KeuanganDAO dao = new KeuanganDAO();
    private final NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JLabel lblTotalSaldo = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblPemasukan = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblPengeluaran = new JLabel("Rp 0", SwingConstants.LEFT);
    private JLabel lblSelisih = new JLabel("Rp 0", SwingConstants.LEFT);
    
    // Label untuk Widget Status Cepat (Target & Utang)
    private JLabel lblStatusTarget = new JLabel("Memuat...", SwingConstants.LEFT);
    private JLabel lblStatusUtang = new JLabel("Memuat...", SwingConstants.LEFT);

    private JTextArea areaRincianDompet = new JTextArea();

    public DashboardFrame(Pengguna u) {
        super("DompetKu • Dashboard Keuangan");
        user = u;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.CHARCOAL);
        top.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel brand = new JLabel("DompetKu");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel hi = new JLabel("Halo, " + u.getNama() + "  ");
        hi.setForeground(Color.WHITE);
        hi.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton logout = new JButton("Keluar");
        logout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logout.setBackground(Theme.PRIMARY);
        logout.setForeground(Color.WHITE);
        logout.setOpaque(true);
        logout.setContentAreaFilled(true);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setFont(new Font("SansSerif", Font.BOLD, 12));
        logout.setBorder(new EmptyBorder(6, 12, 6, 12));
        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(hi);
        right.add(logout);

        top.add(brand, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        tabs.addTab("Ringkasan", summary());
        tabs.addTab("Dompet", wallets());
        tabs.addTab("Transaksi", transactions());
        tabs.addTab("Transfer", transfers());
        tabs.addTab("Anggaran", budgets());
        tabs.addTab("Target Tabungan", savings());
        tabs.addTab("Utang-Piutang", debts());
        tabs.addTab("Laporan & Ekspor", reports());

        shell.add(top, BorderLayout.NORTH);
        shell.add(tabs, BorderLayout.CENTER);
        setContentPane(shell);
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

        // Grid Kartu Statistik Utama (4 Kolom)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsGrid.setOpaque(false);

        cardsGrid.add(createStatCard("Total Saldo", lblTotalSaldo, Theme.TEXT, Theme.WHITE));
        cardsGrid.add(createStatCard("Pemasukan Bulan Ini", lblPemasukan, new Color(15, 81, 50), new Color(212, 237, 218)));
        cardsGrid.add(createStatCard("Pengeluaran Bulan Ini", lblPengeluaran, new Color(114, 28, 36), new Color(248, 215, 218)));
        cardsGrid.add(createStatCard("Selisih / Cashflow", lblSelisih, Theme.GOLD, Theme.WHITE));

        // Widget Status Cepat di bawah kartu statistik (2 Kolom: Target & Utang)
        JPanel statusWidgetsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        statusWidgetsGrid.setOpaque(false);
        statusWidgetsGrid.setBorder(new EmptyBorder(8, 0, 0, 0));

        statusWidgetsGrid.add(createStatCard("Status Target Tabungan", lblStatusTarget, Theme.TEXT, Theme.WHITE));
        statusWidgetsGrid.add(createStatCard("Status Catatan Utang", lblStatusUtang, Theme.TEXT, Theme.WHITE));

        JPanel statsWrapper = new JPanel(new BorderLayout(0, 0));
        statsWrapper.setOpaque(false);
        statsWrapper.add(cardsGrid, BorderLayout.NORTH);
        statsWrapper.add(statusWidgetsGrid, BorderLayout.CENTER);

        topContainer.add(statsWrapper, BorderLayout.CENTER);
        p.add(topContainer, BorderLayout.NORTH);

        // Panel Tengah yang berisi Rincian Dompet & Tips Keuangan
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);

        JPanel walletDetailPanel = new JPanel(new BorderLayout(0, 6));
        walletDetailPanel.setOpaque(false);

        JLabel lblSub = new JLabel("Rincian Dompet & Akun");
        lblSub.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSub.setForeground(Theme.TEXT);
        walletDetailPanel.add(lblSub, BorderLayout.NORTH);

        areaRincianDompet.setEditable(false);
        areaRincianDompet.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaRincianDompet.setForeground(Theme.TEXT);
        areaRincianDompet.setBackground(Theme.WHITE);
        areaRincianDompet.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(areaRincianDompet);
        scroll.getViewport().setBackground(Theme.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        walletDetailPanel.add(scroll, BorderLayout.CENTER);

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

        JLabel textLabel = new JLabel("<html><b>Tips Keuangan Mahasiswa:</b> Biasakan mencatat setiap pengeluaran sekecil apapun dan sisihkan minimal 10% uang saku bulanan di awal untuk pos tabungan darurat.</html>");
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

                    if (daysBetween < 0) {
                        notifPanel.add(createAlertBox("⚠️ PERINGATAN: Utang kepada " + u[2] + " telah LEWAT JATUH TEMPO (" + u[5] + ")!", Theme.EXPENSE));
                    } else if (daysBetween <= 3) {
                        notifPanel.add(createAlertBox("🔔 PERINGATAN: Utang kepada " + u[2] + " akan jatuh tempo dalam " + daysBetween + " hari (" + u[5] + ").", Theme.GOLD));
                    }
                }
            }

            var anggaranList = dao.anggaran(user.getIdPengguna());
            for (String[] a : anggaranList) {
                BigDecimal limit = new BigDecimal(a[4]);
                BigDecimal used = new BigDecimal(a[5]);
                if (limit.signum() > 0) {
                    BigDecimal percentage = used.multiply(new BigDecimal("100")).divide(limit, 0, java.math.RoundingMode.HALF_UP);
                    if (percentage.compareTo(new BigDecimal("100")) >= 0) {
                        notifPanel.add(createAlertBox("🚨 ANGGARAN HABIS: Kategori '" + a[1] + "' telah mencapai " + percentage + "% dari batas anggaran!", Theme.EXPENSE));
                    } else if (percentage.compareTo(new BigDecimal("80")) >= 0) {
                        notifPanel.add(createAlertBox("⚠️ PERINGATAN ANGGARAN: Kategori '" + a[1] + "' sudah terpakai " + percentage + "%!", Theme.GOLD));
                    }
                }
            }
        } catch (Exception ignored) {}

        return notifPanel;
    }

    private JPanel createAlertBox(String message, Color color) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(new Color(255, 240, 240));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(color);
        box.add(lbl, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 6, 0));
        wrapper.add(box, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor, Color backgroundColor) {
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

    private void loadSummary() {
        try {
            StringBuilder b = new StringBuilder();
            b.append(String.format("%-26s %-12s %s%n", "NAMA DOMPET", "JENIS", "SALDO"));
            b.append("---------------------------------------------------------------------\n");

            BigDecimal total = BigDecimal.ZERO;
            for (var w : dao.dompet(user.getIdPengguna())) {
                b.append(String.format("%-26s %-12s %s%n", w.nama(), w.jenis(), money(w.saldo())));
                total = total.add(w.saldo());
            }

            var r = dao.ringkasan(
                    user.getIdPengguna(),
                    YearMonth.now().atDay(1),
                    YearMonth.now().atEndOfMonth());

            lblTotalSaldo.setText(money(total));
            lblPemasukan.setText(money(r.pemasukan()));
            lblPengeluaran.setText(money(r.pengeluaran()));
            lblSelisih.setText(money(r.selisih()));

            // Memuat ringkasan untuk Widget Cepat Target Tabungan
            var targets = dao.target(user.getIdPengguna());
            long activeTargets = targets.stream().filter(t -> !"TERCAPAI".equalsIgnoreCase(t[4])).count();
            lblStatusTarget.setText(targets.isEmpty() ? "Belum ada target" : activeTargets + " Target Aktif Berjalan");

            // Memuat ringkasan untuk Widget Cepat Utang-Piutang
            var debts = dao.utang(user.getIdPengguna());
            long unpaidDebts = debts.stream().filter(d -> !"LUNAS".equalsIgnoreCase(d[6])).count();
            lblStatusUtang.setText(debts.isEmpty() ? "Tidak ada catatan" : unpaidDebts + " Catatan Belum Lunas");

            areaRincianDompet.setText(b.toString());
        } catch (Exception ex) {
            error(ex);
        }
    }

    private JPanel wallets() {
        JPanel p = Theme.page();
        JTable table = new JTable();
        styleTable(table);

        JButton add = Theme.button("+ Tambah Dompet Baru");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(add);

        add.addActionListener(e -> addWallet());

        p.add(Theme.heading("Daftar Dompet & Rekening"), BorderLayout.NORTH);
        p.add(createTableContainer(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);

        loadWallets(table);
        return p;
    }

    private void loadWallets(JTable table) {
        try {
            var rows = dao.dompet(user.getIdPengguna());
            DefaultTableModel m = model("ID", "Nama Dompet", "Jenis", "Saldo");

            for (var r : rows) {
                m.addRow(new Object[]{ r.id(), r.nama(), r.jenis(), money(r.saldo()) });
            }
            table.setModel(m);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addWallet() {
        JTextField nama = new JTextField();
        JComboBox<String> jenis = new JComboBox<>(new String[]{"TUNAI", "BANK", "EWALLET"});
        JTextField saldo = new JTextField("0");

        Object[] fields = {
                "Nama Dompet:", nama,
                "Jenis Akun:", jenis,
                "Saldo Awal (Rp):", saldo
        };

        if (JOptionPane.showConfirmDialog(this, fields, "Tambah Dompet Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            BigDecimal s = amount(saldo.getText());
            Dompet d = switch ((String) jenis.getSelectedItem()) {
                case "BANK" -> new RekeningBank(user.getIdPengguna(), nama.getText(), s);
                case "EWALLET" -> new EWallet(user.getIdPengguna(), nama.getText(), s);
                default -> new DompetTunai(user.getIdPengguna(), nama.getText(), s);
            };

            dao.tambahDompet(d);
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
        JButton find = Theme.button("Cari");
        JButton add = Theme.button("+ Catat Transaksi");
        JButton cat = Theme.button("+ Kelola Kategori");

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Pencarian:"));
        searchBar.add(search);
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

        find.addActionListener(e -> loadTransactions(table, search.getText()));
        add.addActionListener(e -> addTransaction());
        cat.addActionListener(e -> addCategory());

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(actionBar, BorderLayout.NORTH);
        centerPanel.add(createTableContainer(table), BorderLayout.CENTER);

        p.add(Theme.heading("Riwayat Transaksi Keuangan"), BorderLayout.NORTH);
        p.add(centerPanel, BorderLayout.CENTER);

        loadTransactions(table, "");
        return p;
    }

    private void loadTransactions(JTable table, String keyword) {
        try {
            DefaultTableModel m = model("ID", "Tanggal & Waktu", "Jenis", "Dompet", "Kategori", "Nominal", "Keterangan");
            for (var r : dao.riwayat(user.getIdPengguna(), keyword, 300)) {
                m.addRow(new Object[]{
                        r.id(), r.tanggal().format(dateFormatter), r.jenis(), r.dompet(),
                        r.kategori(), money(r.nominal()), r.keterangan()
                });
            }
            table.setModel(m);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addTransaction() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.isEmpty()) {
                info("Buat dompet terlebih dahulu.");
                return;
            }

            JComboBox<String> type = new JComboBox<>(new String[]{"PEMASUKAN", "PENGELUARAN"});
            JComboBox<String> wallet = combo(wallets.stream()
                    .map(w -> w.id() + " | " + w.nama() + " (" + money(w.saldo()) + ")")
                    .toList());

            JTextField nominal = new JTextField();
            JTextField ket = new JTextField();
            JComboBox<String> kategori = new JComboBox<>();

            LocalDate now = LocalDate.now();
            
            String[] days = new String[31];
            for (int i = 0; i < 31; i++) days[i] = String.format("%02d", i + 1);
            JComboBox<String> comboDay = new JComboBox<>(days);
            comboDay.setSelectedItem(String.format("%02d", now.getDayOfMonth()));

            String[] months = {"Januari", "Februari", "Maret", "April", 
                               "Mei", "Juni", "Juli", "Agustus", 
                               "September", "Oktober", "November", "Desember"};
            JComboBox<String> comboMonth = new JComboBox<>(months);
            comboMonth.setSelectedIndex(now.getMonthValue() - 1);

            int currentYear = now.getYear();
            String[] years = {String.valueOf(currentYear), String.valueOf(currentYear - 1), String.valueOf(currentYear - 2)};
            JComboBox<String> comboYear = new JComboBox<>(years);

            JPanel datePanel = new JPanel(new GridLayout(1, 3, 5, 0));
            datePanel.setOpaque(false);
            datePanel.add(comboDay);
            datePanel.add(comboMonth);
            datePanel.add(comboYear);

            Runnable loadCats = () -> {
                kategori.removeAllItems();
                try {
                    for (var k : dao.kategori(user.getIdPengguna(), (String) type.getSelectedItem())) {
                        kategori.addItem(k.id() + " - " + k.nama());
                    }
                } catch (Exception ex) {
                    error(ex);
                }
            };

            kategori.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null) {
                        String text = value.toString();
                        if (text.contains(" - ")) {
                            setText(text.split(" - ", 2)[1]);
                        }
                    }
                    return this;
                }
            });

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

            if (JOptionPane.showConfirmDialog(this, fields, "Catat Transaksi",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            if (wallet.getSelectedItem() == null || kategori.getSelectedItem() == null) {
                info("Pastikan dompet dan kategori telah dipilih.");
                return;
            }

            String selectedKatStr = kategori.getSelectedItem().toString();
            long kid = Long.parseLong(selectedKatStr.split(" - ")[0].trim());

            String selectedDay = (String) comboDay.getSelectedItem();
            int selectedMonthIndex = comboMonth.getSelectedIndex() + 1;
            String selectedMonth = String.format("%02d", selectedMonthIndex);
            String selectedYear = (String) comboYear.getSelectedItem();
            
            LocalDate parsedDate = LocalDate.parse(selectedYear + "-" + selectedMonth + "-" + selectedDay);

            long wid = parseId(wallet.getSelectedItem().toString());

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
        JComboBox<String> t = new JComboBox<>(new String[]{"PEMASUKAN", "PENGELUARAN"});

        if (JOptionPane.showConfirmDialog(this,
                new Object[]{"Nama Kategori:", n, "Jenis Transaksi:", t},
                "Tambah Kategori Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            dao.tambahKategori(user.getIdPengguna(), n.getText(), (String) t.getSelectedItem());
            info("Kategori berhasil ditambahkan.");
        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel transfers() {
        JPanel p = Theme.page();
        p.setLayout(new GridBagLayout());

        JPanel cardForm = new JPanel(new GridBagLayout());
        cardForm.setBackground(Theme.WHITE);
        cardForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(28, 32, 28, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("💸 Transfer Antar Dompet");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Theme.TEXT);
        cardForm.add(title, gbc);

        gbc.gridy++;
        JLabel desc = new JLabel("<html><p style='width: 380px; color: #555;'>Pindahkan saldo antar rekening atau dompet secara real-time dan aman langsung dari workspace Anda.</p></html>");
        cardForm.add(desc, gbc);

        gbc.gridy++;
        JButton doTransfer = Theme.button("Mulai Sesi Transfer");
        doTransfer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        doTransfer.addActionListener(e -> transfer());
        cardForm.add(doTransfer, gbc);

        GridBagConstraints pageGbc = new GridBagConstraints();
        pageGbc.gridx = 0;
        pageGbc.gridy = 0;
        pageGbc.anchor = GridBagConstraints.CENTER;

        p.add(cardForm, pageGbc);
        return p;
    }

    private void transfer() {
        try {
            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.size() < 2) {
                info("Minimal harus ada dua dompet aktif untuk melakukan transfer.");
                return;
            }

            List<String> items = wallets.stream()
                    .map(w -> w.id() + " | " + w.nama() + " (" + money(w.saldo()) + ")")
                    .toList();

            JComboBox<String> src = combo(items, 0);
            JComboBox<String> dst = combo(items, 1);
            JTextField nominal = new JTextField();
            JTextField ket = new JTextField();

            if (JOptionPane.showConfirmDialog(this,
                    new Object[]{
                            "Dompet Sumber (Dari):", src,
                            "Dompet Tujuan (Ke):", dst,
                            "Nominal Transfer (Rp):", nominal,
                            "Catatan/Keterangan:", ket
                    },
                    "Konfirmasi Transfer", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            long sourceId = idFrom(src);
            long destinationId = idFrom(dst);

            if (sourceId == destinationId) {
                info("Dompet sumber dan dompet tujuan tidak boleh sama.");
                return;
            }

            BigDecimal jumlah = amount(nominal.getText());
            String catatan = ket.getText();

            dao.transfer(user.getIdPengguna(), sourceId, destinationId, jumlah, catatan);
            
            String bukti = "========================================\n" +
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

            JOptionPane.showConfirmDialog(this, new JScrollPane(areaBukti), 
                    "Transfer Berhasil - Cetak Bukti", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);

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

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(add);

        p.add(Theme.heading("Anggaran Pengeluaran Bulanan"), BorderLayout.NORTH);
        p.add(createTableContainer(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);

        loadBudgets(table);
        return p;
    }

    private void loadBudgets(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Kategori", "Mulai", "Selesai", "Batas Anggaran", "Terpakai", "Penggunaan");
            for (String[] r : dao.anggaran(user.getIdPengguna())) {
                BigDecimal limit = new BigDecimal(r[4]);
                BigDecimal used = new BigDecimal(r[5]);

                String pct = limit.signum() == 0 ? "-" :
                        used.multiply(new BigDecimal("100")).divide(limit, 1, java.math.RoundingMode.HALF_UP) + "%";

                m.addRow(new Object[]{ r[0], r[1], r[2], r[3], money(limit), money(used), pct });
            }
            table.setModel(m);
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

            JComboBox<String> c = combo(cats.stream().map(k -> k.id() + " | " + k.nama()).toList());
            JTextField mulai = new JTextField(YearMonth.now().atDay(1).toString());
            JTextField akhir = new JTextField(YearMonth.now().atEndOfMonth().toString());
            JTextField batas = new JTextField();

            if (JOptionPane.showConfirmDialog(this,
                    new Object[]{
                            "Kategori Pengeluaran:", c,
                            "Tanggal Mulai (YYYY-MM-DD):", mulai,
                            "Tanggal Selesai (YYYY-MM-DD):", akhir,
                            "Batas Anggaran (Rp):", batas
                    },
                    "Tambah Anggaran Pengeluaran", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            dao.tambahAnggaran(user.getIdPengguna(), idFrom(c), LocalDate.parse(mulai.getText()), LocalDate.parse(akhir.getText()), amount(batas.getText()));
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
        JButton con = Theme.button("+ Tambah Setoran");

        add.addActionListener(e -> addTarget());
        con.addActionListener(e -> contribute());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(con);
        buttons.add(add);

        p.add(Theme.heading("Target Tabungan & Impian"), BorderLayout.NORTH);
        p.add(createTableContainer(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);

        loadTargets(table);
        return p;
    }

    private void loadTargets(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Nama Target", "Target Nominal", "Terkumpul", "Status");
            for (String[] r : dao.target(user.getIdPengguna())) {
                m.addRow(new Object[]{
                        r[0], r[1],
                        money(new BigDecimal(r[2])),
                        money(new BigDecimal(r[3])),
                        r[4]
                });
            }
            table.setModel(m);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addTarget() {
        JTextField n = new JTextField();
        JTextField v = new JTextField();
        JTextField d = new JTextField(LocalDate.now().plusMonths(6).toString());

        if (JOptionPane.showConfirmDialog(this,
                new Object[]{
                        "Nama Impian/Target:", n,
                        "Target Nominal (Rp):", v,
                        "Tenggat Tanggal (YYYY-MM-DD):", d
                },
                "Buat Target Tabungan Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            dao.tambahTarget(user.getIdPengguna(), n.getText(), amount(v.getText()), LocalDate.parse(d.getText()));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void contribute() {
        try {
            var targets = dao.target(user.getIdPengguna());
            if (targets.isEmpty()) {
                info("Buat target tabungan terlebih dahulu.");
                return;
            }

            JComboBox<String> c = combo(targets.stream().map(r -> r[0] + " | " + r[1] + " (" + r[4] + ")").toList());
            JTextField n = new JTextField();
            JTextField k = new JTextField();

            if (JOptionPane.showConfirmDialog(this,
                    new Object[]{
                            "Target Impian:", c,
                            "Nominal Setoran (Rp):", n,
                            "Keterangan/Sumber Dana:", k
                    },
                    "Tambah Setoran Tabungan", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            dao.kontribusi(user.getIdPengguna(), idFrom(c), amount(n.getText()), k.getText());
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
        JButton pay = Theme.button("+ Bayar / Cicil");

        add.addActionListener(e -> addDebt());
        pay.addActionListener(e -> payDebt());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(pay);
        buttons.add(add);

        p.add(Theme.heading("Catatan Utang & Piutang"), BorderLayout.NORTH);
        p.add(createTableContainer(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);

        loadDebts(table);
        return p;
    }

    private void loadDebts(JTable table) {
        try {
            DefaultTableModel m = model("ID", "Jenis", "Nama Pihak", "Nominal Awal", "Sudah Dibayar", "Jatuh Tempo", "Status");
            for (String[] r : dao.utang(user.getIdPengguna())) {
                m.addRow(new Object[]{
                        r[0], r[1], r[2],
                        money(new BigDecimal(r[3])),
                        money(new BigDecimal(r[4])),
                        r[5], r[6]
                });
            }
            table.setModel(m);
        } catch (Exception e) {
            error(e);
        }
    }

    private void addDebt() {
        JComboBox<String> jenis = new JComboBox<>(new String[]{"UTANG", "PIUTANG"});
        JTextField pihak = new JTextField();
        JTextField nom = new JTextField();
        JTextField tempo = new JTextField(LocalDate.now().plusMonths(1).toString());

        if (JOptionPane.showConfirmDialog(this,
                new Object[]{
                        "Jenis Catatan:", jenis,
                        "Nama Pihak (Orang/Lembaga):", pihak,
                        "Nominal Utama (Rp):", nom,
                        "Jatuh Tempo (YYYY-MM-DD):", tempo
                },
                "Catat Utang/Piutang Baru", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            dao.tambahUtang(user.getIdPengguna(), (String) jenis.getSelectedItem(), pihak.getText(), amount(nom.getText()), LocalDate.parse(tempo.getText()));
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private void payDebt() {
        try {
            var rows = dao.utang(user.getIdPengguna());
            if (rows.isEmpty()) {
                info("Belum ada catatan utang/piutang.");
                return;
            }

            var wallets = dao.dompet(user.getIdPengguna());
            if (wallets.isEmpty()) {
                info("Buat dompet terlebih dahulu untuk melakukan pembayaran.");
                return;
            }

            JComboBox<String> c = combo(rows.stream().map(r -> r[0] + " | " + r[1] + " - " + r[2] + " (" + r[6] + ")").toList());
            JComboBox<String> walletCombo = combo(wallets.stream().map(w -> w.id() + " | " + w.nama() + " (" + money(w.saldo()) + ")").toList());
            JTextField n = new JTextField();

            if (JOptionPane.showConfirmDialog(this,
                    new Object[]{
                            "Pilih Catatan Utang/Piutang:", c,
                            "Bayar Menggunakan Dompet:", walletCombo,
                            "Nominal Pembayaran (Rp):", n
                    },
                    "Catat Pembayaran / Angsuran", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            long idUtang = idFrom(c);
            long idDompet = idFrom(walletCombo);
            BigDecimal nominalBayar = amount(n.getText());

            dao.bayarUtang(user.getIdPengguna(), idUtang, nominalBayar, idDompet);
            info("Pembayaran utang berhasil dicatat, saldo dompet terpotong, dan masuk ke riwayat transaksi.");
            refreshAll();
        } catch (Exception e) {
            error(e);
        }
    }

    private JPanel reports() {
        JPanel p = Theme.page();

        JTextField start = new JTextField(YearMonth.now().atDay(1).toString(), 14);
        JTextField end = new JTextField(YearMonth.now().atEndOfMonth().toString(), 14);

        start.setFont(new Font("SansSerif", Font.PLAIN, 15));
        end.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JTextArea out = new JTextArea();
        out.setEditable(false);
        out.setFont(new Font("Monospaced", Font.PLAIN, 14));
        out.setMargin(new Insets(16, 16, 16, 16));

        JButton show = Theme.button("Tampilkan Laporan");
        JButton csv = Theme.button("Ekspor File CSV");

        show.addActionListener(e -> {
            try {
                var r = dao.ringkasan(user.getIdPengguna(), LocalDate.parse(start.getText()), LocalDate.parse(end.getText()));
                out.setText(
                        "====================================================\n" +
                        "               LAPORAN KEUANGAN MAHASISWA           \n" +
                        "====================================================\n" +
                        "Periode Data : " + start.getText() + " s.d. " + end.getText() + "\n\n" +
                        "Total Pemasukan  : " + money(r.pemasukan()) + "\n" +
                        "Total Pengeluaran: " + money(r.pengeluaran()) + "\n" +
                        "----------------------------------------------------\n" +
                        "Selisih Bersih   : " + money(r.selisih()) + "\n" +
                        "====================================================\n"
                );
            } catch (Exception ex) {
                error(ex);
            }
        });

        csv.addActionListener(e -> {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new java.io.File("laporan-dompetku.csv"));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    dao.eksporCsv(user.getIdPengguna(), fc.getSelectedFile().toPath(), LocalDate.parse(start.getText()), LocalDate.parse(end.getText()));
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

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
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

        JScrollPane scroll = new JScrollPane(out);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        reportWrapper.add(scroll, BorderLayout.CENTER);

        p.add(Theme.heading("Laporan Keuangan & Ekspor CSV"), BorderLayout.NORTH);
        p.add(reportWrapper, BorderLayout.CENTER);

        return p;
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
        table.setSelectionBackground(Theme.UBE);
        table.setSelectionForeground(Theme.WHITE);
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
                    JTable tbl, Object value, boolean selected,
                    boolean focus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, selected, focus, row, column);

                label.setOpaque(true);
                label.setBackground(Theme.CHARCOAL);
                label.setForeground(Color.WHITE);
                label.setFont(new Font("SansSerif", Font.BOLD, 13));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.GOLD),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                label.setHorizontalAlignment(SwingConstants.LEFT);

                return label;
            }
        });

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean selected,
                    boolean focus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, selected, focus, row, column);

                label.setFont(new Font("SansSerif", Font.PLAIN, 14));
                label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

                if (selected) {
                    label.setBackground(Theme.UBE);
                    label.setForeground(Theme.WHITE);
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
                        String tipe = jenis.toString().toUpperCase(Locale.ROOT);
                        if (tipe.contains("PEMASUKAN")) {
                            label.setForeground(Theme.INCOME);
                            label.setFont(new Font("SansSerif", Font.BOLD, 14));
                        } else if (tipe.contains("PENGELUARAN")) {
                            label.setForeground(Theme.EXPENSE);
                            label.setFont(new Font("SansSerif", Font.BOLD, 14));
                        }
                    }
                }

                return label;
            }
        });
    }

    private void refreshAll() {
        for (Window w : Window.getWindows()) {
            if (w instanceof DashboardFrame) {
                w.dispose();
                new DashboardFrame(user).setVisible(true);
                return;
            }
        }
    }

    private String money(BigDecimal n) {
        return rupiah.format(n == null ? BigDecimal.ZERO : n);
    }

    private BigDecimal amount(String s) {
        String x = s.trim()
                .replace("Rp", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        BigDecimal b = new BigDecimal(x);

        if (b.signum() <= 0) {
            throw new IllegalArgumentException("Nominal harus lebih dari nol.");
        }

        return b;
    }

    DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    private JComboBox<String> combo(List<String> items) {
        return combo(items, 0);
    }

    private JComboBox<String> combo(List<String> items, int index) {
        JComboBox<String> c = new JComboBox<>(items.toArray(String[]::new));
        if (!items.isEmpty()) {
            c.setSelectedIndex(Math.max(0, Math.min(index, items.size() - 1)));
        }
        return c;
    }

    private long idFrom(JComboBox<String> c) {
        Object selected = c.getSelectedItem();
        if (selected == null) {
            throw new IllegalArgumentException("Pilih item terlebih dahulu.");
        }
        return parseId(selected.toString());
    }

    private long parseId(String text) {
        return Long.parseLong(text.split("\\s*\\|", 2)[0].trim());
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String s) {
        JOptionPane.showMessageDialog(this, s);
    }
}