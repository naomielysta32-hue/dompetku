-- DompetKu PostgreSQL schema. Jalankan pada database dompetku (PostgreSQL 15+).
BEGIN;
CREATE TABLE IF NOT EXISTS pengguna (
 id_pengguna BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nama VARCHAR(120) NOT NULL CHECK (length(trim(nama))>0),
 username VARCHAR(50) NOT NULL UNIQUE CHECK (length(trim(username))>=3),
 password_hash VARCHAR(100) NOT NULL,
 tanggal_daftar TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS dompet (
 id_dompet BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pengguna BIGINT NOT NULL REFERENCES pengguna ON DELETE CASCADE,
 nama_dompet VARCHAR(80) NOT NULL,
 jenis_dompet VARCHAR(20) NOT NULL CHECK (jenis_dompet IN ('TUNAI','BANK','EWALLET')),
 saldo_awal NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (saldo_awal>=0),
 status_aktif BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(id_pengguna,nama_dompet)
);
CREATE TABLE IF NOT EXISTS kategori (
 id_kategori BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pengguna BIGINT REFERENCES pengguna ON DELETE CASCADE,
 nama_kategori VARCHAR(80) NOT NULL,
 jenis_kategori VARCHAR(20) NOT NULL CHECK (jenis_kategori IN ('PEMASUKAN','PENGELUARAN')),
 UNIQUE NULLS NOT DISTINCT(id_pengguna,nama_kategori,jenis_kategori)
);
CREATE TABLE IF NOT EXISTS transaksi (
 id_transaksi BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_dompet BIGINT NOT NULL REFERENCES dompet ON DELETE RESTRICT,
 id_kategori BIGINT REFERENCES kategori ON DELETE SET NULL,
 jenis_transaksi VARCHAR(20) NOT NULL CHECK (jenis_transaksi IN ('PEMASUKAN','PENGELUARAN')),
 nominal NUMERIC(19,2) NOT NULL CHECK(nominal>0),
 tanggal_transaksi TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 keterangan TEXT
);
CREATE TABLE IF NOT EXISTS transfer (
 id_transfer BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_dompet_sumber BIGINT NOT NULL REFERENCES dompet ON DELETE RESTRICT,
 id_dompet_tujuan BIGINT NOT NULL REFERENCES dompet ON DELETE RESTRICT,
 nominal NUMERIC(19,2) NOT NULL CHECK(nominal>0),
 tanggal_transfer TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 keterangan TEXT,
 CHECK(id_dompet_sumber<>id_dompet_tujuan)
);
CREATE TABLE IF NOT EXISTS anggaran (
 id_anggaran BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pengguna BIGINT NOT NULL REFERENCES pengguna ON DELETE CASCADE,
 id_kategori BIGINT NOT NULL REFERENCES kategori ON DELETE RESTRICT,
 periode_mulai DATE NOT NULL, periode_selesai DATE NOT NULL,
 batas_anggaran NUMERIC(19,2) NOT NULL CHECK(batas_anggaran>0),
 CHECK(periode_selesai>=periode_mulai),
 UNIQUE(id_pengguna,id_kategori,periode_mulai,periode_selesai)
);
CREATE TABLE IF NOT EXISTS target_tabungan (
 id_target BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pengguna BIGINT NOT NULL REFERENCES pengguna ON DELETE CASCADE,
 nama_target VARCHAR(120) NOT NULL,
 target_nominal NUMERIC(19,2) NOT NULL CHECK(target_nominal>0),
 tanggal_target DATE,
 status_target VARCHAR(20) NOT NULL DEFAULT 'AKTIF' CHECK(status_target IN ('AKTIF','TERCAPAI','DIBATALKAN'))
);
CREATE TABLE IF NOT EXISTS kontribusi_tabungan (
 id_kontribusi BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_target BIGINT NOT NULL REFERENCES target_tabungan ON DELETE CASCADE,
 nominal NUMERIC(19,2) NOT NULL CHECK(nominal>0),
 tanggal_kontribusi TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 keterangan TEXT
);
CREATE TABLE IF NOT EXISTS utang_piutang (
 id_utang_piutang BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pengguna BIGINT NOT NULL REFERENCES pengguna ON DELETE CASCADE,
 jenis VARCHAR(20) NOT NULL CHECK(jenis IN ('UTANG','PIUTANG')),
 nama_pihak VARCHAR(120) NOT NULL,
 nominal_awal NUMERIC(19,2) NOT NULL CHECK(nominal_awal>0),
 tanggal_catatan DATE NOT NULL DEFAULT CURRENT_DATE,
 tanggal_jatuh_tempo DATE,
 status VARCHAR(20) NOT NULL DEFAULT 'BELUM_LUNAS' CHECK(status IN ('BELUM_LUNAS','SEBAGIAN','LUNAS')),
 CHECK(tanggal_jatuh_tempo IS NULL OR tanggal_jatuh_tempo>=tanggal_catatan)
);
CREATE TABLE IF NOT EXISTS pembayaran_utang_piutang (
 id_pembayaran BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_utang_piutang BIGINT NOT NULL REFERENCES utang_piutang ON DELETE CASCADE,
 nominal_pembayaran NUMERIC(19,2) NOT NULL CHECK(nominal_pembayaran>0),
 tanggal_pembayaran TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 keterangan TEXT
);
CREATE INDEX IF NOT EXISTS idx_dompet_pengguna ON dompet(id_pengguna);
CREATE INDEX IF NOT EXISTS idx_transaksi_dompet_tanggal ON transaksi(id_dompet,tanggal_transaksi DESC);
CREATE INDEX IF NOT EXISTS idx_transfer_sumber ON transfer(id_dompet_sumber,tanggal_transfer DESC);
CREATE INDEX IF NOT EXISTS idx_transfer_tujuan ON transfer(id_dompet_tujuan,tanggal_transfer DESC);
CREATE INDEX IF NOT EXISTS idx_anggaran_pengguna ON anggaran(id_pengguna,periode_mulai,periode_selesai);
CREATE INDEX IF NOT EXISTS idx_target_pengguna ON target_tabungan(id_pengguna,status_target);
CREATE INDEX IF NOT EXISTS idx_utang_pengguna ON utang_piutang(id_pengguna,status);
CREATE INDEX IF NOT EXISTS idx_pembayaran_utang ON pembayaran_utang_piutang(id_utang_piutang,tanggal_pembayaran);
-- Kategori bawaan bersama (id_pengguna NULL)
INSERT INTO kategori(nama_kategori,jenis_kategori) VALUES
 ('Gaji/Beasiswa','PEMASUKAN'),('Hadiah','PEMASUKAN'),('Lainnya','PEMASUKAN'),
 ('Makanan','PENGELUARAN'),('Transportasi','PENGELUARAN'),('Kuliah','PENGELUARAN'),
 ('Tagihan','PENGELUARAN'),('Hiburan','PENGELUARAN'),('Lainnya','PENGELUARAN')
ON CONFLICT DO NOTHING;
COMMIT;
