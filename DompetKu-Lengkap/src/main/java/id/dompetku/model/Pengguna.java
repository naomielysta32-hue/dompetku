package id.dompetku.model;
import java.time.OffsetDateTime;
public class Pengguna {
 private Long idPengguna; private String nama,username,passwordHash; private OffsetDateTime tanggalDaftar;
 public Pengguna(String nama,String username,String passwordHash){this.nama=nama;this.username=username;this.passwordHash=passwordHash;}
 public Long getIdPengguna(){return idPengguna;} public void setIdPengguna(Long v){idPengguna=v;}
 public String getNama(){return nama;} public String getUsername(){return username;}
 public void setNama(String v){nama=v;} public void setUsername(String v){username=v;}
 public String getPasswordHash(){return passwordHash;} public void setTanggalDaftar(OffsetDateTime v){tanggalDaftar=v;}
 public OffsetDateTime getTanggalDaftar(){return tanggalDaftar;}
}
