package id.dompetku.model;
import java.math.BigDecimal;
public final class RekeningBank extends Dompet {
 public RekeningBank(long user,String nama,BigDecimal saldo){super(user,nama,saldo);}
 public String getJenisDompet(){return "BANK";}
}
