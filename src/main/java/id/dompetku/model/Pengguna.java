package id.dompetku.model;

import java.time.OffsetDateTime;

/**
 * Model data akun pengguna.
 */
// [OOP - Encapsulation]: Seluruh data akun pengguna bersifat private dan hanya dapat diakses/diubah lewat getter dan setter.
public class Pengguna {
    private Long idPengguna;
    private String nama;
    private String username;
    private final String passwordHash;
    private OffsetDateTime tanggalDaftar;

    public Pengguna(String nama, String username, String passwordHash) {
        this.nama = nama;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getIdPengguna() { return idPengguna; }
    public void setIdPengguna(Long v) { this.idPengguna = v; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public OffsetDateTime getTanggalDaftar() { return tanggalDaftar; }
    public void setTanggalDaftar(OffsetDateTime v) { this.tanggalDaftar = v; }
}
