package id.dompetku.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model transaksi uang masuk.
 */
// [OOP - Inheritance]: Pemasukan mewarisi seluruh atribut dan validasi dari kelas abstrak Transaksi.
public final class Pemasukan extends Transaksi {
    public Pemasukan(long dompet, Long kategori, BigDecimal nominal, LocalDate tanggal, String ket) {
        super(dompet, kategori, nominal, tanggal, ket);
    }

    // [OOP - Polymorphism]: Method overriding menentukan jenis transaksi PEMASUKAN secara polimorfik.
    @Override
    public String getJenisTransaksi() {
        return "PEMASUKAN";
    }
}
