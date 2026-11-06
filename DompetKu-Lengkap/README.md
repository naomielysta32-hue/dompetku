# DompetKu — Aplikasi Manajemen Keuangan Mahasiswa

Starter aplikasi desktop Java Swing dengan PostgreSQL JDBC, Maven, BCrypt, dan arsitektur paket `config`, `model`, `dao`, `ui`, `util`.

## Persyaratan
- JDK 17+
- Maven 3.8+
- PostgreSQL 15+ (fitur `UNIQUE NULLS NOT DISTINCT`)
- pgAdmin atau DBeaver

## Instalasi database
1. Di pgAdmin/DBeaver, buat database bernama `dompetku`.
2. Buka database `dompetku` lalu jalankan seluruh file `database/dompetku.sql`.
3. Atur environment variable koneksi di terminal PowerShell:

```powershell
$env:DOMPETKU_DB_URL="jdbc:postgresql://localhost:5432/dompetku"
$env:DOMPETKU_DB_USER="postgres"
$env:DOMPETKU_DB_PASSWORD="PASSWORD_POSTGRES_KAMU"
```

Ganti password dengan password PostgreSQL lokal kamu. Jangan memasukkan password ke Git.

## Menjalankan
Dari folder proyek:

```bash
mvn clean package
mvn exec:java
```

Jika Maven tidak memiliki plugin exec di cache, jalankan:
```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=id.dompetku.App
```
Alternatif: build classpath Maven lalu jalankan `id.dompetku.App` dari IDE. IntelliJ IDEA/NetBeans bisa membuka proyek sebagai Maven project.

## Fitur yang tersedia
- Registrasi dan login (hash BCrypt, bukan password plaintext).
- Dompet tunai, rekening bank, e-wallet; saldo dihitung dari saldo awal, transaksi dan transfer.
- Pemasukan/pengeluaran dengan kategori, tanggal dan keterangan.
- Kategori khusus pengguna dan kategori bawaan.
- Riwayat pencarian.
- Transfer antar-dompet atomik menggunakan transaksi JDBC, pengecekan kepemilikan, saldo dan `commit/rollback`.
- Anggaran per kategori, jumlah terpakai, persentase penggunaan.
- Target tabungan dan kontribusi; status target diperbarui saat tercapai.
- Utang-piutang, pencatatan pembayaran/cicilan dan status pelunasan.
- Laporan pemasukan/pengeluaran per periode dan ekspor CSV.
- Antarmuka Swing dengan tab untuk tiap area fitur.

## Catatan desain dan batasan
- Transfer tidak dihitung sebagai pemasukan/pengeluaran pada laporan.
- Jumlah disimpan sebagai `NUMERIC(19,2)` dan diproses dengan `BigDecimal`, bukan `double`.
- DAO memakai `PreparedStatement`; data ditapis berdasarkan pengguna aktif.
- Untuk produksi, disarankan menambahkan automated integration tests, migrasi database terkelola, audit log, pengelolaan sesi, dan pengujian konkurensi.
- Perbandingan antarbulan dapat dibuat dengan memilih dua periode laporan; versi UI starter ini menyediakan laporan per rentang tanggal, belum menampilkan grafik perbandingan visual.
- Penghapusan/edit transaksi dan CRUD penuh untuk seluruh entitas belum disediakan di UI starter; data bisa ditambah dan dilihat untuk fitur-fitur utama.
