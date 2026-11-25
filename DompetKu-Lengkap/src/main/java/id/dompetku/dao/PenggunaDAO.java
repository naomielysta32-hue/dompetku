package id.dompetku.dao;

import id.dompetku.model.Pengguna;
import id.dompetku.util.PasswordUtil;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * DAO (Data Access Object) untuk operasi akun pengguna.
 */
// [OOP - Inheritance]: PenggunaDAO mewarisi helper JDBC (queryOne, executeUpdate, inTransaction) dari BaseDAO.
public class PenggunaDAO extends BaseDAO {

    public record Statistik(int jumlahDompet, int jumlahTransaksi) {}

    // [OOP - Association]: PenggunaDAO menghasilkan dan mengembalikan objek model Pengguna ke layer UI.
    public Pengguna daftar(String nama, String username, String password) throws SQLException {
        if (nama == null || nama.isBlank() || username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Nama dan username valid wajib diisi.");
        }
        String sql = "INSERT INTO pengguna(nama,username,password_hash) VALUES(?,?,?) RETURNING id_pengguna";
        long id = queryOne(sql, r -> r.getLong(1),
                nama.trim(), username.trim(), PasswordUtil.hash(password)).orElseThrow();
        Pengguna u = new Pengguna(nama.trim(), username.trim(), "");
        u.setIdPengguna(id);
        return u;
    }

    public Optional<Pengguna> login(String username, String password) throws SQLException {
        String sql = "SELECT id_pengguna,nama,username,password_hash,tanggal_daftar FROM pengguna WHERE username=?";
        return queryOne(sql, r -> {
            if (!PasswordUtil.verify(password, r.getString("password_hash"))) return null;
            Pengguna u = new Pengguna(r.getString("nama"), r.getString("username"), r.getString("password_hash"));
            u.setIdPengguna(r.getLong("id_pengguna"));
            u.setTanggalDaftar(r.getObject("tanggal_daftar", OffsetDateTime.class));
            return u;
        }, username.trim());
    }

    /** Cek apakah username dan nama lengkap cocok dengan satu akun. */
    public boolean identitasCocok(String username, String nama) throws SQLException {
        return cariIdByIdentitas(username, nama) != null;
    }

    /** Reset password tanpa password lama setelah memverifikasi identitas. */
    public void resetPassword(String username, String nama, String passwordBaru) throws SQLException {
        Long uid = cariIdByIdentitas(username, nama);
        if (uid == null) throw new SQLException("Username atau nama lengkap tidak sesuai.");
        String hashBaru = PasswordUtil.hash(passwordBaru);
        if (executeUpdate("UPDATE pengguna SET password_hash=? WHERE id_pengguna=?", hashBaru, uid) == 0) {
            throw new SQLException("Akun tidak ditemukan.");
        }
    }

    private Long cariIdByIdentitas(String username, String nama) throws SQLException {
        if (username == null || nama == null || username.isBlank() || nama.isBlank()) return null;
        String sql = "SELECT id_pengguna FROM pengguna WHERE username=? AND LOWER(TRIM(regexp_replace(nama,'\\s+',' ','g')))=LOWER(?)";
        return queryOne(sql, r -> r.getLong(1), username.trim(), nama.trim().replaceAll("\\s+", " ")).orElse(null);
    }

    /** Ubah nama dan username. Username harus unik. */
    public void ubahProfil(long uid, String nama, String username) throws SQLException {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama lengkap tidak boleh kosong.");
        if (nama.trim().length() > 120) throw new IllegalArgumentException("Nama lengkap maksimal 120 karakter.");
        if (username == null || username.trim().length() < 3) throw new IllegalArgumentException("Username minimal 3 karakter.");
        if (username.trim().length() > 50) throw new IllegalArgumentException("Username maksimal 50 karakter.");
        try {
            if (executeUpdate("UPDATE pengguna SET nama=?,username=? WHERE id_pengguna=?",
                    nama.trim(), username.trim(), uid) == 0) {
                throw new SQLException("Akun tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw mapUnique(e, "Username sudah dipakai orang lain.");
        }
    }

    /** Ganti password; password lama wajib benar. */
    public void ubahPassword(long uid, String lama, String baru) throws SQLException {
        inTransaction(c -> {
            String hash = queryOne(c, "SELECT password_hash FROM pengguna WHERE id_pengguna=?",
                    r -> r.getString(1), uid).orElseThrow(() -> new SQLException("Akun tidak ditemukan."));
            if (!PasswordUtil.verify(lama, hash)) throw new SQLException("Password lama salah.");
            executeUpdate(c, "UPDATE pengguna SET password_hash=? WHERE id_pengguna=?",
                    PasswordUtil.hash(baru), uid);
        });
    }

    /** Ringkasan angka untuk halaman profil. */
    public Statistik statistik(long uid) throws SQLException {
        String q = """
                SELECT (SELECT COUNT(*) FROM dompet WHERE id_pengguna=? AND status_aktif),
                       (SELECT COUNT(*) FROM transaksi t JOIN dompet d ON d.id_dompet=t.id_dompet WHERE d.id_pengguna=?)
                """;
        return queryOne(q, r -> new Statistik(r.getInt(1), r.getInt(2)), uid, uid).orElseThrow();
    }
}
