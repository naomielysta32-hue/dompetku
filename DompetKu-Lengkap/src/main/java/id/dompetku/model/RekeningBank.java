package id.dompetku.model;

import java.math.BigDecimal;

/**
 * Representasi rekening bank yang disimpan sebagai salah satu jenis dompet.
 */
// [OOP - Inheritance]: RekeningBank mewarisi atribut dan validasi dari kelas induk Dompet melalui super().
public final class RekeningBank extends Dompet {
    public RekeningBank(long user, String nama, BigDecimal saldo) {
        super(user, nama, saldo);
    }

    // [OOP - Polymorphism]: Method overriding mengembalikan identitas jenis BANK.
    @Override
    public String getJenisDompet() {
        return "BANK";
    }
}
