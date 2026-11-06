package id.dompetku.model;
import java.math.BigDecimal;
import java.time.LocalDate;
public abstract class Transaksi {
 private final long idDompet; private final Long idKategori; private final BigDecimal nominal; private final LocalDate tanggal; private final String keterangan;
 protected Transaksi(long dompet,Long kategori,BigDecimal nominal,LocalDate tanggal,String ket){if(dompet<1||nominal==null||nominal.signum()<=0)throw new IllegalArgumentException("Dompet/nominal tidak valid.");this.idDompet=dompet;this.idKategori=kategori;this.nominal=nominal;this.tanggal=tanggal==null?LocalDate.now():tanggal;this.keterangan=ket;}
 public abstract String getJenisTransaksi();
 public long getIdDompet(){return idDompet;} public Long getIdKategori(){return idKategori;} public BigDecimal getNominal(){return nominal;} public LocalDate getTanggal(){return tanggal;} public String getKeterangan(){return keterangan;}
}
