# DompetKu — Aplikasi Manajemen Keuangan Mahasiswa

Aplikasi desktop Java Swing dengan PostgreSQL JDBC, Maven, BCrypt, JCalendar, OpenPDF, dan arsitektur paket `config`, `model`, `dao`, `ui`, `util`.

## Struktur Folder

```text
dompetku/
├── database/
│   └── dompetku.sql                 # Skema tabel & data awal PostgreSQL
├── src/main/java/id/dompetku/
│   ├── App.java                     # Entry point aplikasi Swing
│   ├── config/
│   │   └── DatabaseConnection.java  # Pengelolaan koneksi JDBC PostgreSQL
│   ├── dao/
│   │   ├── BaseDAO.java             # Abstraksi helper JDBC & transaksi
│   │   ├── KeuanganDAO.java         # Akses data dompet, transaksi, anggaran, target, utang, laporan
│   │   └── PenggunaDAO.java         # Autentikasi & manajemen profil pengguna
│   ├── model/
│   │   ├── Dompet.java              # Abstract model dompet (+ DompetTunai, EWallet, RekeningBank)
│   │   ├── Transaksi.java           # Abstract model transaksi (+ Pemasukan, Pengeluaran)
│   │   └── Pengguna.java            # Model data pengguna
│   ├── ui/
│   │   ├── Theme.java               # Sentralisasi warna, font, komponen, filter input, & listener
│   │   ├── Dialogs.java             # Dialog kustom aplikasi
│   │   ├── Sidebar.java             # Navigasi sidebar
│   │   ├── CategoryPiePanel.java    # Grafik pie rincian kategori
│   │   ├── LoginFrame.java          # Halaman login & registrasi
│   │   ├── ProfilePanel.java        # Halaman profil pengguna
│   │   └── DashboardFrame.java      # Dashboard utama & seluruh modul keuangan
│   └── util/
│       └── PasswordUtil.java        # Utilitas hash & verifikasi password (BCrypt)
└── pom.xml                          # Konfigurasi build & dependensi Maven
```

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
- Registrasi, login, dan edit profil (hash BCrypt, bukan password plaintext).
- Dompet tunai, rekening bank, e-wallet (tambah, ubah, hapus); saldo dihitung dari saldo awal, transaksi, dan transfer.
- Pemasukan/pengeluaran dengan kategori, tanggal (kalender), dan keterangan.
- Kategori khusus pengguna dan kategori bawaan.
- Riwayat transaksi dengan pencarian dan filter jenis.
- Transfer antar-dompet atomik menggunakan transaksi JDBC, pengecekan kepemilikan, saldo, `commit/rollback`, dan cetak bukti.
- Anggaran per kategori, jumlah terpakai, persentase penggunaan, dan peringatan batas anggaran.
- Target tabungan dan kontribusi setoran; estimasi tercapai, rekomendasi menabung, dan pembaruan status otomatis.
- Utang-piutang, pencatatan pembayaran/cicilan, notifikasi jatuh tempo, dan status pelunasan.
- Laporan pemasukan/pengeluaran per periode, grafik pie kategori, tren bulanan, ekspor PDF, dan dukungan ekspor CSV.
