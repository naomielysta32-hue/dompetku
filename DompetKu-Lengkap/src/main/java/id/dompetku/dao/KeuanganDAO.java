package id.dompetku.dao;

import id.dompetku.config.DatabaseConnection;
import id.dompetku.model.Dompet;
import id.dompetku.model.Transaksi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * DAO transaksi inti.
 * Saldo dihitung dari saldo awal + seluruh transaksi.
 * Transfer antar-dompet dicatat di tabel transfer dan sebagai sepasang
 * transaksi (Transfer Keluar / Transfer Masuk), sehingga saldo hanya
 * dihitung dari tabel transaksi.
 * Seluruh operasi dibatasi ke dompet milik pengguna aktif.
 */
public class KeuanganDAO {

    private static final String PREFIX_TRANSFER_KELUAR = "Transfer Keluar:";
    private static final String PREFIX_TRANSFER_MASUK = "Transfer Masuk:";

    public record DompetRow(long id, String nama, String jenis,
                            BigDecimal saldo, BigDecimal saldoAwal) {}

    public record KategoriRow(long id, String nama, String jenis) {}

    public record TransaksiRow(long id, LocalDateTime tanggal,
                               String jenis, String dompet,
                               String kategori, BigDecimal nominal,
                               String keterangan) {}

    public record Ringkasan(BigDecimal pemasukan, BigDecimal pengeluaran) {
        public BigDecimal selisih() {
            return pemasukan.subtract(pengeluaran);
        }
    }

    public record KategoriLaporan(String nama, BigDecimal total,
                                  BigDecimal persentase) {}

    public record TrenBulanan(YearMonth bulan,
                              BigDecimal pemasukan,
                              BigDecimal pengeluaran) {}


    // ================= DOMPET =================

    public List<DompetRow> dompet(long uid) throws SQLException {

        String q = """
            SELECT d.id_dompet,d.nama_dompet,d.jenis_dompet,
            d.saldo_awal + COALESCE((
                SELECT SUM(CASE
                    WHEN t.jenis_transaksi='PEMASUKAN'
                    THEN t.nominal ELSE -t.nominal
                END)
                FROM transaksi t
                WHERE t.id_dompet=d.id_dompet
            ),0) saldo,
            d.saldo_awal
            FROM dompet d
            WHERE d.id_pengguna=? AND d.status_aktif
            ORDER BY d.nama_dompet
            """;

        List<DompetRow> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new DompetRow(
                            r.getLong(1),
                            r.getString(2),
                            r.getString(3),
                            r.getBigDecimal(4),
                            r.getBigDecimal(5)
                    ));
                }
            }
        }

        return out;
    }


    public void tambahDompet(Dompet d) throws SQLException {

        String q = """
            INSERT INTO dompet
            (id_pengguna,nama_dompet,jenis_dompet,saldo_awal)
            VALUES(?,?,?,?)
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, d.getIdPengguna());
            p.setString(2, d.getNamaDompet());
            p.setString(3, d.getJenisDompet());
            p.setBigDecimal(4, d.getSaldoAwal());

            p.executeUpdate();

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new SQLException("Nama dompet sudah dipakai.");
            throw e;
        }
    }


    /** Ubah nama, jenis, dan saldo awal. Ditolak bila saldo saat ini jadi negatif. */
    public void ubahDompet(long uid, long id, String nama,
                           String jenis, BigDecimal saldoAwal)
            throws SQLException {

        if (nama == null || nama.isBlank())
            throw new IllegalArgumentException("Nama dompet wajib diisi.");

        if (nama.trim().length() > 80)
            throw new IllegalArgumentException("Nama dompet maksimal 80 karakter.");

        if (!List.of("TUNAI", "BANK", "EWALLET").contains(jenis))
            throw new IllegalArgumentException("Jenis dompet tidak valid.");

        if (saldoAwal == null || saldoAwal.signum() < 0)
            throw new IllegalArgumentException("Saldo awal tidak boleh negatif.");

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                BigDecimal awalLama;

                try (PreparedStatement p = c.prepareStatement(
                        """
                        SELECT saldo_awal FROM dompet
                        WHERE id_dompet=? AND id_pengguna=?
                        AND status_aktif FOR UPDATE
                        """)) {

                    p.setLong(1, id);
                    p.setLong(2, uid);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next())
                            throw new SQLException("Dompet tidak ditemukan.");

                        awalLama = r.getBigDecimal(1);
                    }
                }

                if (saldo(c, id).subtract(awalLama)
                        .add(saldoAwal).signum() < 0) {

                    throw new SQLException(
                            "Saldo awal terlalu kecil: saldo dompet saat ini akan menjadi negatif."
                    );
                }

                try (PreparedStatement p = c.prepareStatement(
                        """
                        UPDATE dompet
                        SET nama_dompet=?,jenis_dompet=?,saldo_awal=?
                        WHERE id_dompet=? AND id_pengguna=?
                        """)) {

                    p.setString(1, nama.trim());
                    p.setString(2, jenis);
                    p.setBigDecimal(3, saldoAwal);
                    p.setLong(4, id);
                    p.setLong(5, uid);

                    p.executeUpdate();
                }

                c.commit();

            } catch (Exception e) {
                c.rollback();

                if (e instanceof SQLException s) {
                    if ("23505".equals(s.getSQLState()))
                        throw new SQLException("Nama dompet sudah dipakai.");
                    throw s;
                }
                if (e instanceof RuntimeException re) throw re;

                throw new SQLException(e);
            }
        }
    }


    /** Jumlah transaksi + transfer yang melibatkan dompet ini. */
    public int jumlahRiwayatDompet(long uid, long id)
            throws SQLException {

        try (Connection c = DatabaseConnection.getConnection()) {
            if (!ownsWallet(c, uid, id))
                throw new SQLException("Dompet tidak ditemukan.");

            return jumlahRiwayat(c, id);
        }
    }


    /**
     * Hapus dompet. Bila belum punya riwayat, dihapus permanen.
     * Bila sudah punya riwayat, dompet hanya diarsipkan (status_aktif=false).
     * @return true jika dihapus permanen, false jika hanya diarsipkan.
     */
    public boolean hapusDompet(long uid, long id)
            throws SQLException {

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                String nama;

                try (PreparedStatement p = c.prepareStatement(
                        """
                        SELECT nama_dompet FROM dompet
                        WHERE id_dompet=? AND id_pengguna=?
                        AND status_aktif FOR UPDATE
                        """)) {

                    p.setLong(1, id);
                    p.setLong(2, uid);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next())
                            throw new SQLException("Dompet tidak ditemukan.");

                        nama = r.getString(1);
                    }
                }

                boolean permanen = jumlahRiwayat(c, id) == 0;

                if (permanen) {
                    try (PreparedStatement p = c.prepareStatement(
                            "DELETE FROM dompet WHERE id_dompet=? AND id_pengguna=?")) {

                        p.setLong(1, id);
                        p.setLong(2, uid);
                        p.executeUpdate();
                    }

                } else {
                    String penanda = " (dihapus #" + id + ")";
                    String dasar = nama.length() > 80 - penanda.length()
                            ? nama.substring(0, 80 - penanda.length())
                            : nama;

                    try (PreparedStatement p = c.prepareStatement(
                            """
                            UPDATE dompet
                            SET status_aktif=FALSE,nama_dompet=?
                            WHERE id_dompet=? AND id_pengguna=?
                            """)) {

                        p.setString(1, dasar + penanda);
                        p.setLong(2, id);
                        p.setLong(3, uid);
                        p.executeUpdate();
                    }
                }

                c.commit();
                return permanen;

            } catch (Exception e) {
                c.rollback();

                if (e instanceof SQLException s) throw s;

                throw new SQLException(e);
            }
        }
    }


    private int jumlahRiwayat(Connection c, long wallet)
            throws SQLException {

        String q = """
            SELECT
            (SELECT COUNT(*) FROM transaksi WHERE id_dompet=?)
            +
            (SELECT COUNT(*) FROM transfer
             WHERE id_dompet_sumber=? OR id_dompet_tujuan=?)
            """;

        try (PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, wallet);
            p.setLong(2, wallet);
            p.setLong(3, wallet);

            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }


    // ================= KATEGORI =================

    public List<KategoriRow> kategori(long uid, String jenis)
            throws SQLException {

        String q = """
            SELECT id_kategori,nama_kategori,jenis_kategori
            FROM kategori
            WHERE jenis_kategori=?
            AND (id_pengguna IS NULL OR id_pengguna=?)
            ORDER BY LOWER(nama_kategori) ASC, id_kategori ASC
            """;

        List<KategoriRow> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setString(1, jenis);
            p.setLong(2, uid);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new KategoriRow(
                            r.getLong(1),
                            r.getString(2),
                            r.getString(3)
                    ));
                }
            }
        }

        return out;
    }


    public void tambahKategori(long uid, String nama, String jenis)
            throws SQLException {

        if (nama == null || nama.isBlank())
            throw new IllegalArgumentException("Nama kategori wajib diisi.");

        if (nama.trim().length() > 80)
            throw new IllegalArgumentException("Nama kategori maksimal 80 karakter.");

        if (!List.of("PEMASUKAN", "PENGELUARAN").contains(jenis))
            throw new IllegalArgumentException("Jenis kategori tidak valid.");

        String q = """
            INSERT INTO kategori
            (id_pengguna,nama_kategori,jenis_kategori)
            VALUES(?,?,?)
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);
            p.setString(2, nama.trim());
            p.setString(3, jenis);

            p.executeUpdate();

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new SQLException("Kategori dengan nama itu sudah ada.");
            throw e;
        }
    }


    // ================= TRANSAKSI =================

    public void tambahTransaksi(long uid, Transaksi t)
            throws SQLException {

        String ketTrx = t.getKeterangan() == null ? "" : t.getKeterangan().trim();

        if (ketTrx.startsWith(PREFIX_TRANSFER_KELUAR) ||
                ketTrx.startsWith(PREFIX_TRANSFER_MASUK)) {

            throw new SQLException(
                    "Keterangan tidak boleh diawali \"Transfer Keluar:\" atau \"Transfer Masuk:\"."
            );
        }

        try (Connection c = DatabaseConnection.getConnection()) {

            if (!ownsWallet(c, uid, t.getIdDompet()))
                throw new SQLException("Dompet bukan milik akun ini.");

            if (t.getIdKategori() != null &&
                    !ownsCategory(c, uid, t.getIdKategori(),
                            t.getJenisTransaksi())) {

                throw new SQLException(
                        "Kategori tidak valid untuk pengguna/jenis transaksi."
                );
            }

            if ("PENGELUARAN".equals(t.getJenisTransaksi()) &&
                    saldo(c, t.getIdDompet()).compareTo(t.getNominal()) < 0) {

                throw new SQLException("Saldo dompet tidak mencukupi.");
            }

            String q = """
                INSERT INTO transaksi
                (id_dompet,id_kategori,jenis_transaksi,
                 nominal,tanggal_transaksi,keterangan)
                VALUES(?,?,?,?,?,?)
                """;

            try (PreparedStatement p = c.prepareStatement(q)) {
                p.setLong(1, t.getIdDompet());

                if (t.getIdKategori() == null)
                    p.setNull(2, Types.BIGINT);
                else
                    p.setLong(2, t.getIdKategori());

                p.setString(3, t.getJenisTransaksi());
                p.setBigDecimal(4, t.getNominal());

                // Tanggal dari pengguna + jam saat ini (urutan riwayat tetap wajar)
                p.setTimestamp(
                        5,
                        Timestamp.valueOf(t.getTanggal().atTime(LocalTime.now()))
                );

                p.setString(6, t.getKeterangan());
                p.executeUpdate();
            }
        }
    }


    // ================= TRANSFER =================

    public void transfer(long uid, long sumber, long tujuan,
                         BigDecimal nominal, String ket)
            throws SQLException {

        if (sumber == tujuan || nominal == null ||
                nominal.signum() <= 0) {

            throw new IllegalArgumentException(
                    "Dompet harus berbeda dan nominal harus positif."
            );
        }

        String k = ket == null ? "" : ket.trim();

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                if (!ownsWallet(c, uid, sumber) ||
                        !ownsWallet(c, uid, tujuan)) {

                    throw new SQLException(
                            "Kedua dompet harus milik akun ini."
                    );
                }

                try (PreparedStatement lock = c.prepareStatement(
                        """
                        SELECT id_dompet FROM dompet
                        WHERE id_dompet IN (?,?)
                        ORDER BY id_dompet FOR UPDATE
                        """)) {

                    lock.setLong(1, sumber);
                    lock.setLong(2, tujuan);

                    try (ResultSet r = lock.executeQuery()) {
                        while (r.next()) {
                            // hanya untuk mengunci baris
                        }
                    }
                }

                if (saldo(c, sumber).compareTo(nominal) < 0)
                    throw new SQLException(
                            "Saldo dompet sumber tidak mencukupi."
                    );

                try (PreparedStatement p = c.prepareStatement(
                        """
                        INSERT INTO transfer
                        (id_dompet_sumber,id_dompet_tujuan,nominal,keterangan)
                        VALUES(?,?,?,?)
                        """)) {

                    p.setLong(1, sumber);
                    p.setLong(2, tujuan);
                    p.setBigDecimal(3, nominal);
                    p.setString(4, k);
                    p.executeUpdate();
                }

                String qTrx = """
                    INSERT INTO transaksi
                    (id_dompet,jenis_transaksi,nominal,
                     tanggal_transaksi,keterangan)
                    VALUES(?,?,?,?,?)
                    """;

                Timestamp now = Timestamp.valueOf(LocalDateTime.now());

                try (PreparedStatement p1 = c.prepareStatement(qTrx)) {
                    p1.setLong(1, sumber);
                    p1.setString(2, "PENGELUARAN");
                    p1.setBigDecimal(3, nominal);
                    p1.setTimestamp(4, now);
                    p1.setString(5, PREFIX_TRANSFER_KELUAR + " " + k);
                    p1.executeUpdate();
                }

                try (PreparedStatement p2 = c.prepareStatement(qTrx)) {
                    p2.setLong(1, tujuan);
                    p2.setString(2, "PEMASUKAN");
                    p2.setBigDecimal(3, nominal);
                    p2.setTimestamp(4, now);
                    p2.setString(5, PREFIX_TRANSFER_MASUK + " " + k);
                    p2.executeUpdate();
                }

                c.commit();

            } catch (Exception ex) {
                c.rollback();

                if (ex instanceof SQLException se) throw se;

                throw new SQLException(ex);
            }
        }
    }


    // ================= VALIDASI =================

    private boolean ownsWallet(Connection c, long uid, long wallet)
            throws SQLException {

        String q = """
            SELECT 1 FROM dompet
            WHERE id_dompet=? AND id_pengguna=? AND status_aktif
            """;

        try (PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, wallet);
            p.setLong(2, uid);

            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }


    private boolean ownsCategory(Connection c, long uid, long cat,
                                 String type) throws SQLException {

        String q = """
            SELECT 1 FROM kategori
            WHERE id_kategori=? AND jenis_kategori=?
            AND (id_pengguna IS NULL OR id_pengguna=?)
            """;

        try (PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, cat);
            p.setString(2, type);
            p.setLong(3, uid);

            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }


    private BigDecimal saldo(Connection c, long wallet)
            throws SQLException {

        String q = """
            SELECT d.saldo_awal + COALESCE((
                SELECT SUM(CASE
                    WHEN jenis_transaksi='PEMASUKAN'
                    THEN nominal ELSE -nominal
                END)
                FROM transaksi
                WHERE id_dompet=d.id_dompet
            ),0)
            FROM dompet d
            WHERE d.id_dompet=?
            """;

        try (PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, wallet);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next())
                    throw new SQLException("Dompet tidak ditemukan.");

                return r.getBigDecimal(1);
            }
        }
    }


    // ================= RIWAYAT =================

    public List<TransaksiRow> riwayat(long uid, String keyword,
                                      int limit) throws SQLException {

        String q = """
            SELECT t.id_transaksi,t.tanggal_transaksi,
            t.jenis_transaksi,d.nama_dompet,
            COALESCE(k.nama_kategori,'-'),
            t.nominal,COALESCE(t.keterangan,'')
            FROM transaksi t
            JOIN dompet d ON d.id_dompet=t.id_dompet
            LEFT JOIN kategori k ON k.id_kategori=t.id_kategori
            WHERE d.id_pengguna=?
            AND (
                LOWER(COALESCE(t.keterangan,'')) LIKE LOWER(?)
                OR LOWER(d.nama_dompet) LIKE LOWER(?)
                OR LOWER(COALESCE(k.nama_kategori,'')) LIKE LOWER(?)
            )
            ORDER BY t.tanggal_transaksi DESC,
                     t.id_transaksi DESC
            LIMIT ?
            """;

        List<TransaksiRow> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);

            String k = "%" +
                    (keyword == null ? "" : keyword.trim()) + "%";

            p.setString(2, k);
            p.setString(3, k);
            p.setString(4, k);
            p.setInt(5, Math.max(1, Math.min(500, limit)));

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new TransaksiRow(
                            r.getLong(1),
                            r.getTimestamp(2).toLocalDateTime(),
                            r.getString(3),
                            r.getString(4),
                            r.getString(5),
                            r.getBigDecimal(6),
                            r.getString(7)
                    ));
                }
            }
        }

        return out;
    }


    // ================= LAPORAN =================

    /** Ringkasan pemasukan/pengeluaran. Transfer antar-dompet tidak dihitung. */
    public Ringkasan ringkasan(long uid, LocalDate mulai,
                               LocalDate akhir) throws SQLException {

        validasiPeriodeLaporan(mulai, akhir);

        String q = """
            SELECT
                COALESCE(SUM(t.nominal) FILTER(
                    WHERE t.jenis_transaksi='PEMASUKAN'
                ),0),

                COALESCE(SUM(t.nominal) FILTER(
                    WHERE t.jenis_transaksi='PENGELUARAN'
                ),0)

            FROM transaksi t
            JOIN dompet d ON d.id_dompet=t.id_dompet

            WHERE d.id_pengguna=?
            AND t.tanggal_transaksi::date BETWEEN ? AND ?

            AND COALESCE(t.keterangan,'')
                NOT LIKE 'Transfer Keluar:%'

            AND COALESCE(t.keterangan,'')
                NOT LIKE 'Transfer Masuk:%'
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);
            p.setObject(2, mulai);
            p.setObject(3, akhir);

            try (ResultSet r = p.executeQuery()) {
                r.next();

                return new Ringkasan(
                        r.getBigDecimal(1),
                        r.getBigDecimal(2)
                );
            }
        }
    }


    /** Rincian pemasukan/pengeluaran per kategori beserta persentasenya. */
    public List<KategoriLaporan> rincianKategori(
            long uid, String jenis,
            LocalDate mulai, LocalDate akhir)
            throws SQLException {

        validasiPeriodeLaporan(mulai, akhir);

        if (!List.of("PEMASUKAN", "PENGELUARAN").contains(jenis))
            throw new IllegalArgumentException(
                    "Jenis transaksi tidak valid."
            );

        String q = """
            SELECT
                COALESCE(k.nama_kategori,'Tanpa Kategori'),
                SUM(t.nominal)

            FROM transaksi t
            JOIN dompet d ON d.id_dompet=t.id_dompet
            LEFT JOIN kategori k ON k.id_kategori=t.id_kategori

            WHERE d.id_pengguna=?
            AND t.jenis_transaksi=?
            AND t.tanggal_transaksi::date BETWEEN ? AND ?

            AND COALESCE(t.keterangan,'')
                NOT LIKE 'Transfer Keluar:%'

            AND COALESCE(t.keterangan,'')
                NOT LIKE 'Transfer Masuk:%'

            GROUP BY COALESCE(k.nama_kategori,'Tanpa Kategori')
            ORDER BY SUM(t.nominal) DESC
            """;

        List<KategoriLaporan> hasil = new ArrayList<>();
        BigDecimal totalSemua = BigDecimal.ZERO;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);
            p.setString(2, jenis);
            p.setObject(3, mulai);
            p.setObject(4, akhir);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    BigDecimal total = r.getBigDecimal(2);

                    totalSemua = totalSemua.add(total);

                    hasil.add(new KategoriLaporan(
                            r.getString(1),
                            total,
                            BigDecimal.ZERO
                    ));
                }
            }
        }

        List<KategoriLaporan> laporan = new ArrayList<>();

        for (KategoriLaporan item : hasil) {

            BigDecimal persen = totalSemua.signum() == 0
                    ? BigDecimal.ZERO
                    : item.total()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalSemua, 1, RoundingMode.HALF_UP);

            laporan.add(new KategoriLaporan(
                    item.nama(),
                    item.total(),
                    persen
            ));
        }

        return laporan;
    }


    /** Tren pemasukan dan pengeluaran per bulan. */
    public List<TrenBulanan> trenBulanan(
            long uid, LocalDate mulai,
            LocalDate akhir) throws SQLException {

        validasiPeriodeLaporan(mulai, akhir);

        String q = """
            WITH daftar_bulan AS (
                SELECT generate_series(
                    date_trunc('month', CAST(? AS date)),
                    date_trunc('month', CAST(? AS date)),
                    INTERVAL '1 month'
                )::date AS bulan
            ),

            rekap AS (
                SELECT
                    date_trunc(
                        'month',t.tanggal_transaksi
                    )::date AS bulan,

                    COALESCE(SUM(t.nominal) FILTER(
                        WHERE t.jenis_transaksi='PEMASUKAN'
                    ),0) AS pemasukan,

                    COALESCE(SUM(t.nominal) FILTER(
                        WHERE t.jenis_transaksi='PENGELUARAN'
                    ),0) AS pengeluaran

                FROM transaksi t
                JOIN dompet d ON d.id_dompet=t.id_dompet

                WHERE d.id_pengguna=?
                AND t.tanggal_transaksi::date BETWEEN ? AND ?

                AND COALESCE(t.keterangan,'')
                    NOT LIKE 'Transfer Keluar:%'

                AND COALESCE(t.keterangan,'')
                    NOT LIKE 'Transfer Masuk:%'

                GROUP BY date_trunc(
                    'month',t.tanggal_transaksi
                )
            )

            SELECT
                b.bulan,
                COALESCE(r.pemasukan,0),
                COALESCE(r.pengeluaran,0)

            FROM daftar_bulan b
            LEFT JOIN rekap r ON r.bulan=b.bulan
            ORDER BY b.bulan
            """;

        List<TrenBulanan> hasil = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setObject(1, mulai);
            p.setObject(2, akhir);
            p.setLong(3, uid);
            p.setObject(4, mulai);
            p.setObject(5, akhir);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    hasil.add(new TrenBulanan(
                            YearMonth.from(r.getDate(1).toLocalDate()),
                            r.getBigDecimal(2),
                            r.getBigDecimal(3)
                    ));
                }
            }
        }

        return hasil;
    }


    private void validasiPeriodeLaporan(
            LocalDate mulai, LocalDate akhir) {

        if (mulai == null || akhir == null ||
                akhir.isBefore(mulai)) {

            throw new IllegalArgumentException(
                    "Periode tanggal tidak valid."
            );
        }
    }


    // ================= ANGGARAN =================

    public List<String[]> anggaran(long uid) throws SQLException {

        String q = """
            SELECT a.id_anggaran,k.nama_kategori,
            a.periode_mulai,a.periode_selesai,a.batas_anggaran,

            COALESCE((
                SELECT SUM(t.nominal)
                FROM transaksi t
                JOIN dompet d ON d.id_dompet=t.id_dompet
                WHERE d.id_pengguna=a.id_pengguna
                AND t.id_kategori=a.id_kategori
                AND t.jenis_transaksi='PENGELUARAN'
                AND t.tanggal_transaksi::date
                    BETWEEN a.periode_mulai AND a.periode_selesai
            ),0) terpakai

            FROM anggaran a
            JOIN kategori k ON k.id_kategori=a.id_kategori
            WHERE a.id_pengguna=?
            ORDER BY a.periode_mulai DESC
            """;

        List<String[]> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new String[]{
                            r.getString(1),
                            r.getString(2),
                            r.getString(3),
                            r.getString(4),
                            r.getString(5),
                            r.getString(6)
                    });
                }
            }
        }

        return out;
    }


    public void tambahAnggaran(long uid, long kategori,
                               LocalDate mulai, LocalDate akhir,
                               BigDecimal batas) throws SQLException {

        if (mulai == null || akhir == null || akhir.isBefore(mulai))
            throw new IllegalArgumentException("Periode anggaran tidak valid.");

        if (batas == null || batas.signum() <= 0)
            throw new IllegalArgumentException("Batas anggaran harus positif.");

        String q = """
            INSERT INTO anggaran
            (id_pengguna,id_kategori,periode_mulai,
             periode_selesai,batas_anggaran)
            VALUES(?,?,?,?,?)
            """;

        try (Connection c = DatabaseConnection.getConnection()) {

            if (!ownsCategory(c, uid, kategori, "PENGELUARAN"))
                throw new SQLException("Kategori pengeluaran tidak valid.");

            try (PreparedStatement p = c.prepareStatement(q)) {
                p.setLong(1, uid);
                p.setLong(2, kategori);
                p.setObject(3, mulai);
                p.setObject(4, akhir);
                p.setBigDecimal(5, batas);

                p.executeUpdate();
            }

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new SQLException(
                        "Anggaran untuk kategori dan periode itu sudah ada."
                );
            throw e;
        }
    }


    // ================= TARGET TABUNGAN =================

    public void tambahTarget(long uid, String nama,
                             BigDecimal target, LocalDate tanggal)
            throws SQLException {

        if (nama == null || nama.isBlank())
            throw new IllegalArgumentException("Nama target wajib diisi.");

        if (nama.trim().length() > 120)
            throw new IllegalArgumentException("Nama target maksimal 120 karakter.");

        if (target == null || target.signum() <= 0)
            throw new IllegalArgumentException("Nominal target harus positif.");

        String q = """
            INSERT INTO target_tabungan
            (id_pengguna,nama_target,target_nominal,tanggal_target)
            VALUES(?,?,?,?)
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);
            p.setString(2, nama.trim());
            p.setBigDecimal(3, target);
            p.setObject(4, tanggal);

            p.executeUpdate();
        }
    }


    public List<String[]> target(long uid) throws SQLException {

        String q = """
            SELECT s.id_target,s.nama_target,s.target_nominal,
            COALESCE(SUM(k.nominal),0),s.status_target

            FROM target_tabungan s
            LEFT JOIN kontribusi_tabungan k
                ON k.id_target=s.id_target

            WHERE s.id_pengguna=?

            GROUP BY s.id_target,s.nama_target,
                     s.target_nominal,s.status_target

            ORDER BY s.id_target DESC
            """;

        List<String[]> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new String[]{
                            r.getString(1),
                            r.getString(2),
                            r.getString(3),
                            r.getString(4),
                            r.getString(5)
                    });
                }
            }
        }

        return out;
    }


    public void kontribusi(long uid, long target,
                           BigDecimal nominal, String ket)
            throws SQLException {

        if (nominal == null || nominal.signum() <= 0)
            throw new IllegalArgumentException(
                    "Nominal kontribusi harus positif."
            );

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                try (PreparedStatement p = c.prepareStatement(
                        """
                        SELECT id_target FROM target_tabungan
                        WHERE id_target=? AND id_pengguna=?
                        FOR UPDATE
                        """)) {

                    p.setLong(1, target);
                    p.setLong(2, uid);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next())
                            throw new SQLException("Target tidak ditemukan.");
                    }
                }

                try (PreparedStatement p = c.prepareStatement(
                        """
                        INSERT INTO kontribusi_tabungan
                        (id_target,nominal,keterangan)
                        VALUES(?,?,?)
                        """)) {

                    p.setLong(1, target);
                    p.setBigDecimal(2, nominal);
                    p.setString(3, ket);
                    p.executeUpdate();
                }

                try (PreparedStatement p = c.prepareStatement(
                        """
                        UPDATE target_tabungan
                        SET status_target=CASE
                            WHEN (
                                SELECT COALESCE(SUM(nominal),0)
                                FROM kontribusi_tabungan
                                WHERE id_target=?
                            )>=target_nominal
                            THEN 'TERCAPAI'
                            ELSE 'AKTIF'
                        END
                        WHERE id_target=?
                        """)) {

                    p.setLong(1, target);
                    p.setLong(2, target);
                    p.executeUpdate();
                }

                c.commit();

            } catch (Exception e) {
                c.rollback();

                if (e instanceof SQLException s) throw s;

                throw new SQLException(e);
            }
        }
    }


    // ================= UTANG PIUTANG =================

    public void tambahUtang(long uid, String jenis, String pihak,
                            BigDecimal nominal, LocalDate tempo)
            throws SQLException {

        if (!List.of("UTANG", "PIUTANG").contains(jenis))
            throw new IllegalArgumentException("Jenis harus UTANG atau PIUTANG.");

        if (pihak == null || pihak.isBlank())
            throw new IllegalArgumentException("Nama pihak wajib diisi.");

        if (pihak.trim().length() > 120)
            throw new IllegalArgumentException("Nama pihak maksimal 120 karakter.");

        if (nominal == null || nominal.signum() <= 0)
            throw new IllegalArgumentException("Nominal harus positif.");

        if (tempo != null && tempo.isBefore(LocalDate.now()))
            throw new IllegalArgumentException(
                    "Jatuh tempo tidak boleh sebelum hari ini."
            );

        String q = """
            INSERT INTO utang_piutang
            (id_pengguna,jenis,nama_pihak,
             nominal_awal,tanggal_jatuh_tempo)
            VALUES(?,?,?,?,?)
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);
            p.setString(2, jenis);
            p.setString(3, pihak.trim());
            p.setBigDecimal(4, nominal);
            p.setObject(5, tempo);

            p.executeUpdate();
        }
    }


    public List<String[]> utang(long uid) throws SQLException {

        String q = """
            SELECT u.id_utang_piutang,u.jenis,u.nama_pihak,
            u.nominal_awal,
            COALESCE(SUM(p.nominal_pembayaran),0),
            u.tanggal_jatuh_tempo,u.status

            FROM utang_piutang u
            LEFT JOIN pembayaran_utang_piutang p
                ON p.id_utang_piutang=u.id_utang_piutang

            WHERE u.id_pengguna=?

            GROUP BY u.id_utang_piutang,u.jenis,u.nama_pihak,
                     u.nominal_awal,u.tanggal_jatuh_tempo,u.status

            ORDER BY u.id_utang_piutang DESC
            """;

        List<String[]> out = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {

            p.setLong(1, uid);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.add(new String[]{
                            r.getString(1),
                            r.getString(2),
                            r.getString(3),
                            r.getString(4),
                            r.getString(5),
                            r.getString(6),
                            r.getString(7)
                    });
                }
            }
        }

        return out;
    }


    /**
     * Catat pembayaran/angsuran. Bila idDompet diisi, dibuat juga transaksi:
     * UTANG = pengeluaran (saldo dicek), PIUTANG = pemasukan.
     * Bila idDompet null, hanya pembayaran yang dicatat.
     */
    public void bayarUtang(long uid, long id,
                           BigDecimal nominal, Long idDompet)
            throws SQLException {

        if (nominal == null || nominal.signum() <= 0)
            throw new IllegalArgumentException(
                    "Nominal pembayaran harus positif."
            );

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                BigDecimal awal, terbayar;
                String pihak, jenis;

                try (PreparedStatement p = c.prepareStatement(
                        """
                        SELECT nominal_awal,nama_pihak,jenis
                        FROM utang_piutang
                        WHERE id_utang_piutang=? AND id_pengguna=?
                        FOR UPDATE
                        """)) {

                    p.setLong(1, id);
                    p.setLong(2, uid);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next())
                            throw new SQLException("Catatan tidak ditemukan.");

                        awal = r.getBigDecimal(1);
                        pihak = r.getString(2);
                        jenis = r.getString(3);
                    }
                }

                try (PreparedStatement p = c.prepareStatement(
                        """
                        SELECT COALESCE(SUM(nominal_pembayaran),0)
                        FROM pembayaran_utang_piutang
                        WHERE id_utang_piutang=?
                        """)) {

                    p.setLong(1, id);

                    try (ResultSet r = p.executeQuery()) {
                        r.next();
                        terbayar = r.getBigDecimal(1);
                    }
                }

                BigDecimal total = terbayar.add(nominal);

                if (total.compareTo(awal) > 0)
                    throw new SQLException(
                            "Nominal pembayaran melebihi sisa."
                    );

                try (PreparedStatement p = c.prepareStatement(
                        """
                        INSERT INTO pembayaran_utang_piutang
                        (id_utang_piutang,nominal_pembayaran,keterangan)
                        VALUES(?,?,?)
                        """)) {

                    p.setLong(1, id);
                    p.setBigDecimal(2, nominal);
                    p.setString(3, "Pembayaran cicilan");
                    p.executeUpdate();
                }

                try (PreparedStatement p = c.prepareStatement(
                        """
                        UPDATE utang_piutang SET status=?
                        WHERE id_utang_piutang=?
                        """)) {

                    p.setString(
                            1,
                            total.compareTo(awal) == 0 ? "LUNAS" : "SEBAGIAN"
                    );

                    p.setLong(2, id);
                    p.executeUpdate();
                }

                if (idDompet != null) {

                    if (!ownsWallet(c, uid, idDompet))
                        throw new SQLException("Dompet tidak valid.");

                    boolean utang = "UTANG".equals(jenis);

                    if (utang && saldo(c, idDompet).compareTo(nominal) < 0)
                        throw new SQLException(
                                "Saldo dompet tidak mencukupi untuk membayar utang."
                        );

                    try (PreparedStatement pTrx = c.prepareStatement(
                            """
                            INSERT INTO transaksi
                            (id_dompet,jenis_transaksi,nominal,
                             tanggal_transaksi,keterangan)
                            VALUES(?,?,?,?,?)
                            """)) {

                        pTrx.setLong(1, idDompet);
                        pTrx.setString(2, utang ? "PENGELUARAN" : "PEMASUKAN");
                        pTrx.setBigDecimal(3, nominal);
                        pTrx.setTimestamp(
                                4,
                                Timestamp.valueOf(LocalDateTime.now())
                        );
                        pTrx.setString(
                                5,
                                (utang ? "Pembayaran Utang ke "
                                        : "Penerimaan Piutang dari ") + pihak
                        );

                        pTrx.executeUpdate();
                    }
                }

                c.commit();

            } catch (Exception e) {
                c.rollback();

                if (e instanceof SQLException s) throw s;

                throw new SQLException(e);
            }
        }
    }


    // ================= EKSPOR CSV =================

    public void eksporCsv(long uid, java.nio.file.Path path,
                          LocalDate mulai, LocalDate akhir)
            throws Exception {

        validasiPeriodeLaporan(mulai, akhir);

        String q = """
            SELECT t.tanggal_transaksi::date,
            t.jenis_transaksi,d.nama_dompet,
            COALESCE(k.nama_kategori,''),
            t.nominal,COALESCE(t.keterangan,'')

            FROM transaksi t
            JOIN dompet d ON d.id_dompet=t.id_dompet
            LEFT JOIN kategori k ON k.id_kategori=t.id_kategori

            WHERE d.id_pengguna=?
            AND t.tanggal_transaksi::date BETWEEN ? AND ?

            ORDER BY t.tanggal_transaksi
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q);
             java.io.BufferedWriter w =
                     java.nio.file.Files.newBufferedWriter(
                             path,
                             java.nio.charset.StandardCharsets.UTF_8
                     )) {

            p.setLong(1, uid);
            p.setObject(2, mulai);
            p.setObject(3, akhir);

            w.write("tanggal,jenis,dompet,kategori,nominal,keterangan\n");

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    w.write(
                            csv(r.getString(1)) + "," +
                                    csv(r.getString(2)) + "," +
                                    csv(r.getString(3)) + "," +
                                    csv(r.getString(4)) + "," +
                                    csv(r.getString(5)) + "," +
                                    csv(r.getString(6))
                    );

                    w.newLine();
                }
            }
        }
    }


    private String csv(String s) {
        return "\"" +
                (s == null ? "" : s.replace("\"", "\"\"")) +
                "\"";
    }
}