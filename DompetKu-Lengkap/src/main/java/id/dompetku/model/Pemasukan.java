package id.dompetku.model;
import java.math.BigDecimal; import java.time.LocalDate;
public final class Pemasukan extends Transaksi {
 public Pemasukan(long dompet,Long kategori,BigDecimal nominal,LocalDate tanggal,String ket){super(dompet,kategori,nominal,tanggal,ket);}
 public String getJenisTransaksi(){return "PEMASUKAN";}
}
