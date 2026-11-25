package id.dompetku.model;

import java.math.BigDecimal;

/**
 * Representasi dompet digital (E-Wallet).
 */
// [OOP - Inheritance]: EWallet mewarisi perilaku umum dan validasi data dari kelas induk Dompet.
public final class EWallet extends Dompet {
    public EWallet(long user, String nama, BigDecimal saldo) {
        super(user, nama, saldo);
    }

    // [OOP - Polymorphism]: Method overriding menyediakan implementasi spesifik jenis EWALLET.
    @Override
    public String getJenisDompet() {
        return "EWALLET";
    }
}
