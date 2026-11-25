package id.dompetku.model;

import java.math.BigDecimal;

/**
 * Kelas induk abstrak untuk semua jenis dompet.
 */
// [OOP - Abstraction]: Abstract class Dompet mendefinisikan kontrak umum dompet tanpa mengimplementasikan jenis spesifiknya.
public abstract class Dompet {
    // [OOP - Encapsulation]: Seluruh atribut dibuat private agar tidak dapat dimodifikasi langsung dari luar kelas.
    private Long idDompet;
    private final long idPengguna;
    private final String namaDompet;
    private final BigDecimal saldoAwal;

    protected Dompet(long user, String nama, BigDecimal saldo) {
        if (user < 1 || nama == null || nama.isBlank() || saldo == null || saldo.signum() < 0) {
            throw new IllegalArgumentException("Data dompet tidak valid.");
        }
        this.idPengguna = user;
        this.namaDompet = nama.trim();
        this.saldoAwal = saldo;
    }

    // [OOP - Abstraction]: Abstract method yang wajib diimplementasikan setiap subclass untuk menentukan jenis dompet.
    public abstract String getJenisDompet();

    // [OOP - Encapsulation]: Getter dan setter mengontrol akses baca/tulis terhadap atribut private.
    public Long getIdDompet() { return idDompet; }
    public void setIdDompet(Long v) { this.idDompet = v; }
    public long getIdPengguna() { return idPengguna; }
    public String getNamaDompet() { return namaDompet; }
    public BigDecimal getSaldoAwal() { return saldoAwal; }
}
