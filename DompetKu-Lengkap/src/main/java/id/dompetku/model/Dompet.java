package id.dompetku.model;
import java.math.BigDecimal;
public abstract class Dompet {
 private Long idDompet; private final long idPengguna; private final String namaDompet; private final BigDecimal saldoAwal;
 protected Dompet(long user,String nama,BigDecimal saldo){if(user<1||nama==null||nama.isBlank()||saldo==null||saldo.signum()<0)throw new IllegalArgumentException("Data dompet tidak valid.");idPengguna=user;namaDompet=nama.trim();saldoAwal=saldo;}
 public abstract String getJenisDompet();
 public Long getIdDompet(){return idDompet;} public void setIdDompet(Long v){idDompet=v;}
 public long getIdPengguna(){return idPengguna;} public String getNamaDompet(){return namaDompet;} public BigDecimal getSaldoAwal(){return saldoAwal;}
}
