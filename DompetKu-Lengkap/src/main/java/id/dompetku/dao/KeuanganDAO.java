package id.dompetku.dao;

import id.dompetku.config.DatabaseConnection;
import id.dompetku.model.Dompet;
import id.dompetku.model.Transaksi;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * DAO (Data Access Object) untuk seluruh operasi keuangan aplikasi DompetKu.
 */
// [OOP - Inheritance]: KeuanganDAO mewarisi method utilitas queryList, queryOne, executeUpdate, dan inTransaction dari BaseDAO.
public class KeuanganDAO extends BaseDAO {

    private static final String PREFIX_TRANSFER_KELUAR = "Transfer Keluar:";
    private static final String PREFIX_TRANSFER_MASUK = "Transfer Masuk:";

    // [OOP - Encapsulation]: Record membungkus baris hasil query menjadi struktur data immutable yang ringkas.
    public record DompetRow(long id, String nama, String jenis, BigDecimal saldo, BigDecimal saldoAwal) {}
    public record KategoriRow(long id, String nama, String jenis) {}
    public record TransaksiRow(long id, LocalDateTime tanggal, String jenis, String dompet,
                               String kategori, BigDecimal nominal, String keterangan) {}
    public record Ringkasan(BigDecimal pemasukan, BigDecimal pengeluaran) {
        public BigDecimal selisih() { return pemasukan.subtract(pengeluaran); }
    }
    public record KategoriLaporan(String nama, BigDecimal total, BigDecimal persentase) {}
    public record TrenBulanan(YearMonth bulan, BigDecimal pemasukan, BigDecimal pengeluaran) {}

    // ================= VALIDASI =================

    /** Nama dompet hanya boleh huruf (boleh ada spasi di antara kata). */
    public static void validasiNamaDompet(String nama) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama dompet wajib diisi.");
        String n = nama.trim();
        if (n.length() > 80) throw new IllegalArgumentException("Nama dompet maksimal 80 karakter.");
        if (!n.matches("\\p{L}+( \\p{L}+)*")) {
            throw new IllegalArgumentException("Nama dompet hanya boleh berisi huruf (tanpa angka atau simbol).");
        }
    }

    private static void validasiPeriodeLaporan(LocalDate mulai, LocalDate akhir) {
        if (mulai == null || akhir == null || akhir.isBefore(mulai)) {
            throw new IllegalArgumentException("Periode tanggal tidak valid.");
        }
    }

    private static void validasiJenisTransaksi(String jenis) {
        if (!"PEMASUKAN".equals(jenis) && !"PENGELUARAN".equals(jenis)) {
            throw new IllegalArgumentException("Jenis transaksi tidak valid.");
        }
    }

    // ================= DOMPET =================

    public List<DompetRow> dompet(long uid) throws SQLException {
        String q = """
                SELECT d.id_dompet, d.nama_dompet, d.jenis_dompet,
                       d.saldo_awal + COALESCE((
                           SELECT SUM(CASE WHEN t.jenis_transaksi='PEMASUKAN' THEN t.nominal ELSE -t.nominal END)
                           FROM transaksi t WHERE t.id_dompet=d.id_dompet
                       ), 0) AS saldo,
                       d.saldo_awal
                FROM dompet d WHERE d.id_pengguna=? AND d.status_aktif ORDER BY d.nama_dompet
                """;
        return queryList(q, r -> new DompetRow(
                r.getLong(1), r.getString(2), r.getString(3), r.getBigDecimal(4), r.getBigDecimal(5)), uid);
    }

    // [OOP - Polymorphism]: Parameter bertipe induk Dompet dapat menerima subclass DompetTunai, EWallet, atau RekeningBank.
    public void tambahDompet(Dompet d) throws SQLException {
        validasiNamaDompet(d.getNamaDompet());
        String q = "INSERT INTO dompet(id_pengguna,nama_dompet,jenis_dompet,saldo_awal) VALUES(?,?,?,?)";
        try {
            executeUpdate(q, d.getIdPengguna(), d.getNamaDompet().trim(), d.getJenisDompet(), d.getSaldoAwal());
        } catch (SQLException e) {
            throw mapUnique(e, "Nama dompet sudah dipakai.");
        }
    }

    /** Ubah nama, jenis, dan saldo awal. Ditolak bila saldo saat ini jadi negatif. */
    public void ubahDompet(long uid, long id, String nama, String jenis, BigDecimal saldoAwal) throws SQLException {
        validasiNamaDompet(nama);
        if (!List.of("TUNAI", "BANK", "EWALLET").contains(jenis)) throw new IllegalArgumentException("Jenis dompet tidak valid.");
        if (saldoAwal == null || saldoAwal.signum() < 0) throw new IllegalArgumentException("Saldo awal tidak boleh negatif.");
        try {
            inTransaction(c -> {
                BigDecimal awalLama = queryOne(c,
                        "SELECT saldo_awal FROM dompet WHERE id_dompet=? AND id_pengguna=? AND status_aktif FOR UPDATE",
                        r -> r.getBigDecimal(1), id, uid)
                        .orElseThrow(() -> new SQLException("Dompet tidak ditemukan."));
                if (saldo(c, id).subtract(awalLama).add(saldoAwal).signum() < 0) {
                    throw new SQLException("Saldo awal terlalu kecil: saldo dompet saat ini akan menjadi negatif.");
                }
                executeUpdate(c, "UPDATE dompet SET nama_dompet=?,jenis_dompet=?,saldo_awal=? WHERE id_dompet=? AND id_pengguna=?",
                        nama.trim(), jenis, saldoAwal, id, uid);
            });
        } catch (SQLException e) {
            throw mapUnique(e, "Nama dompet sudah dipakai.");
        }
    }

    /** Jumlah transaksi + transfer yang melibatkan dompet ini. */
    public int jumlahRiwayatDompet(long uid, long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!ownsWallet(c, uid, id)) throw new SQLException("Dompet tidak ditemukan.");
            return jumlahRiwayat(c, id);
        }
    }

    /** Hapus dompet permanen jika belum ada riwayat, atau arsipkan jika sudah pernah bertransaksi. */
    public boolean hapusDompet(long uid, long id) throws SQLException {
        return inTransaction(c -> {
            String nama = queryOne(c,
                    "SELECT nama_dompet FROM dompet WHERE id_dompet=? AND id_pengguna=? AND status_aktif FOR UPDATE",
                    r -> r.getString(1), id, uid)
                    .orElseThrow(() -> new SQLException("Dompet tidak ditemukan."));
            boolean permanen = jumlahRiwayat(c, id) == 0;
            if (permanen) {
                executeUpdate(c, "DELETE FROM dompet WHERE id_dompet=? AND id_pengguna=?", id, uid);
            } else {
                String penanda = " (dihapus #" + id + ")";
                String dasar = nama.length() > 80 - penanda.length() ? nama.substring(0, 80 - penanda.length()) : nama;
                executeUpdate(c, "UPDATE dompet SET status_aktif=FALSE,nama_dompet=? WHERE id_dompet=? AND id_pengguna=?",
                        dasar + penanda, id, uid);
            }
            return permanen;
        });
    }

    private int jumlahRiwayat(Connection c, long wallet) throws SQLException {
        String q = "SELECT (SELECT COUNT(*) FROM transaksi WHERE id_dompet=?) + "
                + "(SELECT COUNT(*) FROM transfer WHERE id_dompet_sumber=? OR id_dompet_tujuan=?)";
        return queryOne(c, q, r -> r.getInt(1), wallet, wallet, wallet).orElse(0);
    }

    // ================= KATEGORI =================

    public List<KategoriRow> kategori(long uid, String jenis) throws SQLException {
        String q = """
                SELECT id_kategori,nama_kategori,jenis_kategori FROM kategori
                WHERE jenis_kategori=? AND (id_pengguna IS NULL OR id_pengguna=?)
                ORDER BY LOWER(nama_kategori) ASC, id_kategori ASC
                """;
        return queryList(q, r -> new KategoriRow(r.getLong(1), r.getString(2), r.getString(3)), jenis, uid);
    }

    public void tambahKategori(long uid, String nama, String jenis) throws SQLException {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama kategori wajib diisi.");
        if (nama.trim().length() > 80) throw new IllegalArgumentException("Nama kategori maksimal 80 karakter.");
        if (!List.of("PEMASUKAN", "PENGELUARAN").contains(jenis)) throw new IllegalArgumentException("Jenis kategori tidak valid.");
        try {
            executeUpdate("INSERT INTO kategori(id_pengguna,nama_kategori,jenis_kategori) VALUES(?,?,?)",
                    uid, nama.trim(), jenis);
        } catch (SQLException e) {
            throw mapUnique(e, "Kategori dengan nama itu sudah ada.");
        }
    }

    // ================= TRANSAKSI & TRANSFER =================

    // [OOP - Polymorphism]: Parameter Transaksi memproses Pemasukan maupun Pengeluaran lewat method getJenisTransaksi().
    public void tambahTransaksi(long uid, Transaksi t) throws SQLException {
        String ketTrx = t.getKeterangan() == null ? "" : t.getKeterangan().trim();
        if (ketTrx.startsWith(PREFIX_TRANSFER_KELUAR) || ketTrx.startsWith(PREFIX_TRANSFER_MASUK)) {
            throw new SQLException("Keterangan tidak boleh diawali \"Transfer Keluar:\" atau \"Transfer Masuk:\".");
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!ownsWallet(c, uid, t.getIdDompet())) throw new SQLException("Dompet bukan milik akun ini.");
            if (t.getIdKategori() != null && !ownsCategory(c, uid, t.getIdKategori(), t.getJenisTransaksi())) {
                throw new SQLException("Kategori tidak valid untuk pengguna/jenis transaksi.");
            }
            if ("PENGELUARAN".equals(t.getJenisTransaksi()) && saldo(c, t.getIdDompet()).compareTo(t.getNominal()) < 0) {
                throw new SQLException("Saldo dompet tidak mencukupi.");
            }
            insertTransaksi(c, t.getIdDompet(), t.getIdKategori(), t.getJenisTransaksi(),
                    t.getNominal(), t.getTanggal().atTime(LocalTime.now()), t.getKeterangan());
        }
    }

    public void transfer(long uid, long sumber, long tujuan, BigDecimal nominal, String ket) throws SQLException {
        if (sumber == tujuan || nominal == null || nominal.signum() <= 0) {
            throw new IllegalArgumentException("Dompet harus berbeda dan nominal harus positif.");
        }
        String k = ket == null ? "" : ket.trim();
        inTransaction(c -> {
            if (!ownsWallet(c, uid, sumber) || !ownsWallet(c, uid, tujuan)) {
                throw new SQLException("Kedua dompet harus milik akun ini.");
            }
            queryList(c, "SELECT id_dompet FROM dompet WHERE id_dompet IN (?,?) ORDER BY id_dompet FOR UPDATE",
                    r -> r.getLong(1), sumber, tujuan);
            if (saldo(c, sumber).compareTo(nominal) < 0) throw new SQLException("Saldo dompet sumber tidak mencukupi.");

            executeUpdate(c, "INSERT INTO transfer(id_dompet_sumber,id_dompet_tujuan,nominal,keterangan) VALUES(?,?,?,?)",
                    sumber, tujuan, nominal, k);
            LocalDateTime now = LocalDateTime.now();
            insertTransaksi(c, sumber, null, "PENGELUARAN", nominal, now, PREFIX_TRANSFER_KELUAR + " " + k);
            insertTransaksi(c, tujuan, null, "PEMASUKAN", nominal, now, PREFIX_TRANSFER_MASUK + " " + k);
        });
    }

    private void insertTransaksi(Connection c, long idDompet, Long idKategori, String jenis,
                                 BigDecimal nominal, LocalDateTime waktu, String ket) throws SQLException {
        String q = "INSERT INTO transaksi(id_dompet,id_kategori,jenis_transaksi,nominal,tanggal_transaksi,keterangan) VALUES(?,?,?,?,?,?)";
        executeUpdate(c, q, idDompet, idKategori, jenis, nominal, waktu, ket);
    }

    private boolean ownsWallet(Connection c, long uid, long wallet) throws SQLException {
        return queryOne(c, "SELECT 1 FROM dompet WHERE id_dompet=? AND id_pengguna=? AND status_aktif",
                r -> true, wallet, uid).isPresent();
    }

    private boolean ownsCategory(Connection c, long uid, long cat, String type) throws SQLException {
        return queryOne(c, "SELECT 1 FROM kategori WHERE id_kategori=? AND jenis_kategori=? AND (id_pengguna IS NULL OR id_pengguna=?)",
                r -> true, cat, type, uid).isPresent();
    }

    private BigDecimal saldo(Connection c, long wallet) throws SQLException {
        String q = """
                SELECT d.saldo_awal + COALESCE((
                    SELECT SUM(CASE WHEN jenis_transaksi='PEMASUKAN' THEN nominal ELSE -nominal END)
                    FROM transaksi WHERE id_dompet=d.id_dompet
                ), 0) FROM dompet d WHERE d.id_dompet=?
                """;
        return queryOne(c, q, r -> r.getBigDecimal(1), wallet)
                .orElseThrow(() -> new SQLException("Dompet tidak ditemukan."));
    }

    // ================= RIWAYAT =================

    // [OOP - Polymorphism]: Method overloading riwayat tanpa filter jenis meneruskan ke versi berparameter lengkap.
    public List<TransaksiRow> riwayat(long uid, String keyword, int limit) throws SQLException {
        return riwayat(uid, keyword, null, limit);
    }

    public List<TransaksiRow> riwayat(long uid, String keyword, String jenis, int limit) throws SQLException {
        boolean filterJenis = jenis != null && !jenis.isBlank();
        if (filterJenis) validasiJenisTransaksi(jenis);

        String q = """
                SELECT t.id_transaksi, t.tanggal_transaksi, t.jenis_transaksi, d.nama_dompet,
                       COALESCE(k.nama_kategori,'-'), t.nominal, COALESCE(t.keterangan,'')
                FROM transaksi t
                JOIN dompet d ON d.id_dompet=t.id_dompet
                LEFT JOIN kategori k ON k.id_kategori=t.id_kategori
                WHERE d.id_pengguna=?
                  AND (LOWER(COALESCE(t.keterangan,'')) LIKE LOWER(?)
                       OR LOWER(d.nama_dompet) LIKE LOWER(?)
                       OR LOWER(COALESCE(k.nama_kategori,'')) LIKE LOWER(?))
                """ + (filterJenis ? " AND t.jenis_transaksi=? " : "") + """
                ORDER BY t.tanggal_transaksi DESC, t.id_transaksi DESC LIMIT ?
                """;
        String k = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Object> params = new ArrayList<>(List.of(uid, k, k, k));
        if (filterJenis) params.add(jenis);
        params.add(Math.max(1, Math.min(500, limit)));

        return queryList(q, r -> new TransaksiRow(
                r.getLong(1), r.getTimestamp(2).toLocalDateTime(), r.getString(3),
                r.getString(4), r.getString(5), r.getBigDecimal(6), r.getString(7)), params.toArray());
    }

    // ================= LAPORAN =================

    public Ringkasan ringkasan(long uid, LocalDate mulai, LocalDate akhir) throws SQLException {
        validasiPeriodeLaporan(mulai, akhir);
        String q = """
                SELECT COALESCE(SUM(t.nominal) FILTER(WHERE t.jenis_transaksi='PEMASUKAN'), 0),
                       COALESCE(SUM(t.nominal) FILTER(WHERE t.jenis_transaksi='PENGELUARAN'), 0)
                FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet
                WHERE d.id_pengguna=? AND t.tanggal_transaksi::date BETWEEN ? AND ?
                  AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Keluar:%'
                  AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Masuk:%'
                """;
        return queryOne(q, r -> new Ringkasan(r.getBigDecimal(1), r.getBigDecimal(2)), uid, mulai, akhir).orElseThrow();
    }

    public List<KategoriLaporan> rincianKategori(long uid, String jenis, LocalDate mulai, LocalDate akhir) throws SQLException {
        validasiPeriodeLaporan(mulai, akhir);
        validasiJenisTransaksi(jenis);
        String q = """
                SELECT COALESCE(k.nama_kategori,'Tanpa Kategori'), SUM(t.nominal)
                FROM transaksi t
                JOIN dompet d ON d.id_dompet=t.id_dompet
                LEFT JOIN kategori k ON k.id_kategori=t.id_kategori
                WHERE d.id_pengguna=? AND t.jenis_transaksi=? AND t.tanggal_transaksi::date BETWEEN ? AND ?
                  AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Keluar:%'
                  AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Masuk:%'
                GROUP BY COALESCE(k.nama_kategori,'Tanpa Kategori') ORDER BY SUM(t.nominal) DESC
                """;
        List<KategoriLaporan> mentah = queryList(q, r -> new KategoriLaporan(r.getString(1), r.getBigDecimal(2), BigDecimal.ZERO),
                uid, jenis, mulai, akhir);
        BigDecimal totalSemua = mentah.stream().map(KategoriLaporan::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return mentah.stream().map(item -> {
            BigDecimal persen = totalSemua.signum() == 0 ? BigDecimal.ZERO
                    : item.total().multiply(BigDecimal.valueOf(100)).divide(totalSemua, 1, RoundingMode.HALF_UP);
            return new KategoriLaporan(item.nama(), item.total(), persen);
        }).toList();
    }

    public List<TrenBulanan> trenBulanan(long uid, LocalDate mulai, LocalDate akhir) throws SQLException {
        validasiPeriodeLaporan(mulai, akhir);
        String q = """
                WITH daftar_bulan AS (
                    SELECT generate_series(date_trunc('month', CAST(? AS date)),
                                           date_trunc('month', CAST(? AS date)), INTERVAL '1 month')::date AS bulan
                ), rekap AS (
                    SELECT date_trunc('month', t.tanggal_transaksi)::date AS bulan,
                           COALESCE(SUM(t.nominal) FILTER(WHERE t.jenis_transaksi='PEMASUKAN'), 0) AS pemasukan,
                           COALESCE(SUM(t.nominal) FILTER(WHERE t.jenis_transaksi='PENGELUARAN'), 0) AS pengeluaran
                    FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet
                    WHERE d.id_pengguna=? AND t.tanggal_transaksi::date BETWEEN ? AND ?
                      AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Keluar:%'
                      AND COALESCE(t.keterangan,'') NOT LIKE 'Transfer Masuk:%'
                    GROUP BY date_trunc('month', t.tanggal_transaksi)
                )
                SELECT b.bulan, COALESCE(r.pemasukan,0), COALESCE(r.pengeluaran,0)
                FROM daftar_bulan b LEFT JOIN rekap r ON r.bulan=b.bulan ORDER BY b.bulan
                """;
        return queryList(q, r -> new TrenBulanan(
                YearMonth.from(r.getDate(1).toLocalDate()), r.getBigDecimal(2), r.getBigDecimal(3)),
                mulai, akhir, uid, mulai, akhir);
    }

    // ================= ANGGARAN =================

    public List<String[]> anggaran(long uid) throws SQLException {
        String q = """
                SELECT a.id_anggaran, k.nama_kategori, a.periode_mulai, a.periode_selesai, a.batas_anggaran,
                       COALESCE((SELECT SUM(t.nominal) FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet
                                 WHERE d.id_pengguna=a.id_pengguna AND t.id_kategori=a.id_kategori
                                   AND t.jenis_transaksi='PENGELUARAN'
                                   AND t.tanggal_transaksi::date BETWEEN a.periode_mulai AND a.periode_selesai), 0) AS terpakai
                FROM anggaran a JOIN kategori k ON k.id_kategori=a.id_kategori
                WHERE a.id_pengguna=? ORDER BY a.periode_mulai DESC
                """;
        return queryList(q, r -> rowStrings(r, 6), uid);
    }

    public void tambahAnggaran(long uid, long kategori, LocalDate mulai, LocalDate akhir, BigDecimal batas) throws SQLException {
        if (mulai == null || akhir == null || akhir.isBefore(mulai)) throw new IllegalArgumentException("Periode anggaran tidak valid.");
        if (batas == null || batas.signum() <= 0) throw new IllegalArgumentException("Batas anggaran harus positif.");
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!ownsCategory(c, uid, kategori, "PENGELUARAN")) throw new SQLException("Kategori pengeluaran tidak valid.");
            executeUpdate(c, "INSERT INTO anggaran(id_pengguna,id_kategori,periode_mulai,periode_selesai,batas_anggaran) VALUES(?,?,?,?,?)",
                    uid, kategori, mulai, akhir, batas);
        } catch (SQLException e) {
            throw mapUnique(e, "Anggaran untuk kategori dan periode itu sudah ada.");
        }
    }

    // ================= TARGET TABUNGAN =================

    public void tambahTarget(long uid, String nama, BigDecimal target, LocalDate tanggal) throws SQLException {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama target wajib diisi.");
        if (nama.trim().length() > 120) throw new IllegalArgumentException("Nama target maksimal 120 karakter.");
        if (target == null || target.signum() <= 0) throw new IllegalArgumentException("Nominal target harus positif.");
        executeUpdate("INSERT INTO target_tabungan(id_pengguna,nama_target,target_nominal,tanggal_target) VALUES(?,?,?,?)",
                uid, nama.trim(), target, tanggal);
    }

    public List<String[]> target(long uid) throws SQLException {
        String q = """
                SELECT s.id_target, s.nama_target, s.target_nominal, COALESCE(SUM(k.nominal),0), s.status_target,
                       s.tanggal_target, MIN(k.tanggal_kontribusi)::date, MAX(k.tanggal_kontribusi)::date,
                       COUNT(DISTINCT k.tanggal_kontribusi::date)
                FROM target_tabungan s LEFT JOIN kontribusi_tabungan k ON k.id_target=s.id_target
                WHERE s.id_pengguna=?
                GROUP BY s.id_target, s.nama_target, s.target_nominal, s.status_target, s.tanggal_target
                ORDER BY s.id_target DESC
                """;
        return queryList(q, r -> rowStrings(r, 9), uid);
    }

    public void kontribusi(long uid, long target, BigDecimal nominal, String ket) throws SQLException {
        if (nominal == null || nominal.signum() <= 0) throw new IllegalArgumentException("Nominal kontribusi harus positif.");
        inTransaction(c -> {
            queryOne(c, "SELECT id_target FROM target_tabungan WHERE id_target=? AND id_pengguna=? FOR UPDATE",
                    r -> r.getLong(1), target, uid).orElseThrow(() -> new SQLException("Target tidak ditemukan."));
            executeUpdate(c, "INSERT INTO kontribusi_tabungan(id_target,nominal,keterangan) VALUES(?,?,?)",
                    target, nominal, ket);
            String updateStatus = """
                    UPDATE target_tabungan SET status_target=CASE
                        WHEN (SELECT COALESCE(SUM(nominal),0) FROM kontribusi_tabungan WHERE id_target=?) >= target_nominal
                        THEN 'TERCAPAI' ELSE 'AKTIF' END
                    WHERE id_target=?
                    """;
            executeUpdate(c, updateStatus, target, target);
        });
    }

    // ================= UTANG PIUTANG =================

    public void tambahUtang(long uid, String jenis, String pihak, BigDecimal nominal, LocalDate tempo) throws SQLException {
        if (!List.of("UTANG", "PIUTANG").contains(jenis)) throw new IllegalArgumentException("Jenis harus UTANG atau PIUTANG.");
        if (pihak == null || pihak.isBlank()) throw new IllegalArgumentException("Nama pihak wajib diisi.");
        if (pihak.trim().length() > 120) throw new IllegalArgumentException("Nama pihak maksimal 120 karakter.");
        if (nominal == null || nominal.signum() <= 0) throw new IllegalArgumentException("Nominal harus positif.");
        if (tempo != null && tempo.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Jatuh tempo tidak boleh sebelum hari ini.");
        }
        executeUpdate("INSERT INTO utang_piutang(id_pengguna,jenis,nama_pihak,nominal_awal,tanggal_jatuh_tempo) VALUES(?,?,?,?,?)",
                uid, jenis, pihak.trim(), nominal, tempo);
    }

    public List<String[]> utang(long uid) throws SQLException {
        String q = """
                SELECT u.id_utang_piutang, u.jenis, u.nama_pihak, u.nominal_awal,
                       COALESCE(SUM(p.nominal_pembayaran),0), u.tanggal_jatuh_tempo, u.status
                FROM utang_piutang u
                LEFT JOIN pembayaran_utang_piutang p ON p.id_utang_piutang=u.id_utang_piutang
                WHERE u.id_pengguna=?
                GROUP BY u.id_utang_piutang, u.jenis, u.nama_pihak, u.nominal_awal, u.tanggal_jatuh_tempo, u.status
                ORDER BY u.id_utang_piutang DESC
                """;
        return queryList(q, r -> rowStrings(r, 7), uid);
    }

    /** Catat pembayaran/angsuran; jika idDompet diisi, dibuat juga transaksi penyesuai saldo. */
    public void bayarUtang(long uid, long id, BigDecimal nominal, Long idDompet) throws SQLException {
        if (nominal == null || nominal.signum() <= 0) throw new IllegalArgumentException("Nominal pembayaran harus positif.");
        inTransaction(c -> {
            Object[] info = queryOne(c, "SELECT nominal_awal,nama_pihak,jenis FROM utang_piutang WHERE id_utang_piutang=? AND id_pengguna=? FOR UPDATE",
                    r -> new Object[]{r.getBigDecimal(1), r.getString(2), r.getString(3)}, id, uid)
                    .orElseThrow(() -> new SQLException("Catatan tidak ditemukan."));
            BigDecimal awal = (BigDecimal) info[0];
            String pihak = (String) info[1];
            String jenis = (String) info[2];

            BigDecimal terbayar = queryOne(c, "SELECT COALESCE(SUM(nominal_pembayaran),0) FROM pembayaran_utang_piutang WHERE id_utang_piutang=?",
                    r -> r.getBigDecimal(1), id).orElse(BigDecimal.ZERO);
            BigDecimal total = terbayar.add(nominal);
            if (total.compareTo(awal) > 0) throw new SQLException("Nominal pembayaran melebihi sisa.");

            executeUpdate(c, "INSERT INTO pembayaran_utang_piutang(id_utang_piutang,nominal_pembayaran,keterangan) VALUES(?,?,?)",
                    id, nominal, "Pembayaran cicilan");
            executeUpdate(c, "UPDATE utang_piutang SET status=? WHERE id_utang_piutang=?",
                    total.compareTo(awal) == 0 ? "LUNAS" : "SEBAGIAN", id);

            if (idDompet != null) {
                if (!ownsWallet(c, uid, idDompet)) throw new SQLException("Dompet tidak valid.");
                boolean utang = "UTANG".equals(jenis);
                if (utang && saldo(c, idDompet).compareTo(nominal) < 0) {
                    throw new SQLException("Saldo dompet tidak mencukupi untuk membayar utang.");
                }
                insertTransaksi(c, idDompet, null, utang ? "PENGELUARAN" : "PEMASUKAN", nominal,
                        LocalDateTime.now(), (utang ? "Pembayaran Utang ke " : "Penerimaan Piutang dari ") + pihak);
            }
        });
    }

    // ================= EKSPOR CSV =================

    public void eksporCsv(long uid, Path path, LocalDate mulai, LocalDate akhir) throws Exception {
        validasiPeriodeLaporan(mulai, akhir);
        String q = """
                SELECT t.tanggal_transaksi::date, t.jenis_transaksi, d.nama_dompet,
                       COALESCE(k.nama_kategori,''), t.nominal, COALESCE(t.keterangan,'')
                FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet
                LEFT JOIN kategori k ON k.id_kategori=t.id_kategori
                WHERE d.id_pengguna=? AND t.tanggal_transaksi::date BETWEEN ? AND ?
                ORDER BY t.tanggal_transaksi
                """;
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("tanggal,jenis,dompet,kategori,nominal,keterangan\n");
            for (String[] r : queryList(q, rs -> rowStrings(rs, 6), uid, mulai, akhir)) {
                w.write(String.join(",", Arrays.stream(r).map(this::csv).toArray(String[]::new)));
                w.newLine();
            }
        }
    }

    private String csv(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }
}
