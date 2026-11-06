package id.dompetku.model;
import java.math.BigDecimal;
public final class DompetTunai extends Dompet {
 public DompetTunai(long user,String nama,BigDecimal saldo){super(user,nama,saldo);}
 public String getJenisDompet(){return "TUNAI";}
}
