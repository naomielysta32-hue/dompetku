package id.dompetku.model;

import java.math.BigDecimal;

/**
 * Representasi dompet uang tunai.
 */
// [OOP - Inheritance]: DompetTunai mewarisi atribut dan validasi konstruktor dari kelas induk Dompet.
public final class DompetTunai extends Dompet {
    public DompetTunai(long user, String nama, BigDecimal saldo) {
        super(user, nama, saldo);
    }

    // [OOP - Polymorphism]: Method overriding mengimplementasikan getJenisDompet() khusus untuk dompet tunai.
    @Override
    public String getJenisDompet() {
        return "TUNAI";
    }
}
