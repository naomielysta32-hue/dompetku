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

 // ==========================================================
 // LUPA PASSWORD (verifikasi username + nama lengkap)
 // ==========================================================

 /**
  * Cek apakah username dan nama lengkap cocok dengan satu akun.
  * Username harus sama persis seperti saat login. Nama tidak peduli
  * huruf besar/kecil dan spasi berlebih.
  */
 public boolean identitasCocok(String username,String nama)throws SQLException{
  return cariIdByIdentitas(username,nama)!=null;
 }

 /**
  * Reset password tanpa password lama. Identitas diverifikasi ulang di sini,
  * jadi method ini aman dipanggil langsung.
  * Melempar IllegalArgumentException bila password baru < 8 karakter.
  */
 public void resetPassword(String username,String nama,String passwordBaru)throws SQLException{
  Long uid=cariIdByIdentitas(username,nama);
  if(uid==null)throw new SQLException("Username atau nama lengkap tidak sesuai.");
  String hashBaru=PasswordUtil.hash(passwordBaru);
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE pengguna SET password_hash=? WHERE id_pengguna=?")){
   p.setString(1,hashBaru);p.setLong(2,uid);
   if(p.executeUpdate()==0)throw new SQLException("Akun tidak ditemukan.");
  }
 }

 private Long cariIdByIdentitas(String username,String nama)throws SQLException{
  if(username==null||nama==null||username.isBlank()||nama.isBlank())return null;
  String sql="SELECT id_pengguna FROM pengguna WHERE username=? AND LOWER(TRIM(regexp_replace(nama,'\\s+',' ','g')))=LOWER(?)";
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){
   p.setString(1,username.trim());
   p.setString(2,nama.trim().replaceAll("\\s+"," "));
   try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):null;}
  }
 }

 public record Statistik(int jumlahDompet,int jumlahTransaksi){}

 /** Ubah nama dan username. Username harus unik. */
 public void ubahProfil(long uid,String nama,String username)throws SQLException{
  if(nama==null||nama.isBlank())throw new IllegalArgumentException("Nama lengkap tidak boleh kosong.");
  if(nama.trim().length()>120)throw new IllegalArgumentException("Nama lengkap maksimal 120 karakter.");
  if(username==null||username.trim().length()<3)throw new IllegalArgumentException("Username minimal 3 karakter.");
  if(username.trim().length()>50)throw new IllegalArgumentException("Username maksimal 50 karakter.");
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE pengguna SET nama=?,username=? WHERE id_pengguna=?")){
   p.setString(1,nama.trim());p.setString(2,username.trim());p.setLong(3,uid);
   if(p.executeUpdate()==0)throw new SQLException("Akun tidak ditemukan.");
  }catch(SQLException e){
   if("23505".equals(e.getSQLState()))throw new SQLException("Username sudah dipakai orang lain.");
   throw e;
  }
 }

 /** Ganti password; password lama wajib benar. */
 public void ubahPassword(long uid,String lama,String baru)throws SQLException{
  try(Connection c=DatabaseConnection.getConnection()){
   String hash;
   try(PreparedStatement p=c.prepareStatement("SELECT password_hash FROM pengguna WHERE id_pengguna=?")){
    p.setLong(1,uid);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Akun tidak ditemukan.");hash=r.getString(1);}
   }
   if(!PasswordUtil.verify(lama,hash))throw new SQLException("Password lama salah.");
   String hashBaru=PasswordUtil.hash(baru); // melempar IllegalArgumentException bila < 8 karakter
   try(PreparedStatement p=c.prepareStatement("UPDATE pengguna SET password_hash=? WHERE id_pengguna=?")){
    p.setString(1,hashBaru);p.setLong(2,uid);p.executeUpdate();
   }
  }
 }

 /** Ringkasan angka untuk halaman profil. */
 public Statistik statistik(long uid)throws SQLException{
  String q="SELECT (SELECT COUNT(*) FROM dompet WHERE id_pengguna=? AND status_aktif),(SELECT COUNT(*) FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet WHERE d.id_pengguna=?)";
  try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(q)){
   p.setLong(1,uid);p.setLong(2,uid);
   try(ResultSet r=p.executeQuery()){r.next();return new Statistik(r.getInt(1),r.getInt(2));}
  }
 }
}