package id.dompetku.dao;
import id.dompetku.config.DatabaseConnection; import id.dompetku.model.Pengguna; import id.dompetku.util.PasswordUtil;
import java.sql.*; import java.util.Optional;
/** DAO untuk registrasi dan login. */
public class PenggunaDAO {
 public Pengguna daftar(String nama,String username,String password)throws SQLException{
  if(nama==null||nama.isBlank()||username==null||username.trim().length()<3)throw new IllegalArgumentException("Nama dan username valid wajib diisi.");
  String sql="INSERT INTO pengguna(nama,username,password_hash) VALUES(?,?,?) RETURNING id_pengguna";
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){
   p.setString(1,nama.trim());p.setString(2,username.trim());p.setString(3,PasswordUtil.hash(password));
   try(ResultSet r=p.executeQuery()){r.next();Pengguna u=new Pengguna(nama.trim(),username.trim(),"");u.setIdPengguna(r.getLong(1));return u;}
  }
 }
 public Optional<Pengguna> login(String username,String password)throws SQLException{
  String sql="SELECT id_pengguna,nama,username,password_hash,tanggal_daftar FROM pengguna WHERE username=?";
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){
   p.setString(1,username.trim());try(ResultSet r=p.executeQuery()){
    if(!r.next()||!PasswordUtil.verify(password,r.getString("password_hash")))return Optional.empty();
    Pengguna u=new Pengguna(r.getString("nama"),r.getString("username"),r.getString("password_hash"));u.setIdPengguna(r.getLong("id_pengguna"));u.setTanggalDaftar(r.getObject("tanggal_daftar",java.time.OffsetDateTime.class));return Optional.of(u);
   }
  }
 }
}
