package id.dompetku.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model transaksi uang keluar.
 */
// [OOP - Inheritance]: Pengeluaran mewarisi struktur data dan validasi umum dari kelas abstrak Transaksi.
public final class Pengeluaran extends Transaksi {
    public Pengeluaran(long dompet, Long kategori, BigDecimal nominal, LocalDate tanggal, String ket) {
        super(dompet, kategori, nominal, tanggal, ket);
    }

    // [OOP - Polymorphism]: Method overriding mengembalikan jenis transaksi PENGELUARAN.
    @Override
    public String getJenisTransaksi() {
        return "PENGELUARAN";
    }
}
