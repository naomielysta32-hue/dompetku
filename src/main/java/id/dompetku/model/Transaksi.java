package id.dompetku.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Kelas induk abstrak untuk transaksi keuangan.
 */
// [OOP - Abstraction]: Abstract class Transaksi memodelkan konsep umum transaksi dan mewajibkan subclass menentukan jenisnya.
public abstract class Transaksi {
    // [OOP - Encapsulation]: Field private final melindungi integritas data transaksi setelah objek dibuat.
    private final long idDompet;
    private final Long idKategori;
    private final BigDecimal nominal;
    private final LocalDate tanggal;
    private final String keterangan;

    protected Transaksi(long dompet, Long kategori, BigDecimal nominal, LocalDate tanggal, String ket) {
        if (dompet < 1 || nominal == null || nominal.signum() <= 0) {
            throw new IllegalArgumentException("Dompet/nominal tidak valid.");
        }
        this.idDompet = dompet;
        this.idKategori = kategori;
        this.nominal = nominal;
        this.tanggal = tanggal == null ? LocalDate.now() : tanggal;
        this.keterangan = ket;
    }

    // [OOP - Abstraction]: Abstract method yang menentukan tipe transaksi (PEMASUKAN atau PENGELUARAN) pada subclass.
    public abstract String getJenisTransaksi();

    // [OOP - Encapsulation]: Akses baca atribut disediakan melalui method getter.
    public long getIdDompet() { return idDompet; }
    public Long getIdKategori() { return idKategori; }
    public BigDecimal getNominal() { return nominal; }
    public LocalDate getTanggal() { return tanggal; }
    public String getKeterangan() { return keterangan; }
}
