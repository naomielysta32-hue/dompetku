package id.dompetku.model;
import java.math.BigDecimal;
public final class EWallet extends Dompet {
 public EWallet(long user,String nama,BigDecimal saldo){super(user,nama,saldo);}
 public String getJenisDompet(){return "EWALLET";}
}
