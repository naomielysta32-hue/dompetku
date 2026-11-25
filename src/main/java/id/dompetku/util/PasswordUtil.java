package id.dompetku.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitas keamanan password menggunakan algoritma hash BCrypt.
 */
// [OOP - Encapsulation]: Constructor private mencegah pembuatan objek; detail hashing dan verifikasi dibungkus dalam method static.
public final class PasswordUtil {
    private PasswordUtil() {}

    /** Membuat hash BCrypt baru untuk disimpan ke database. */
    public static String hash(String s) {
        if (s == null || s.length() < 8) {
            throw new IllegalArgumentException("Password minimal 8 karakter.");
        }
        return BCrypt.hashpw(s, BCrypt.gensalt(12));
    }

    /** Membandingkan password masukan dengan hash yang tersimpan. */
    public static boolean verify(String s, String hash) {
        try {
            return s != null && hash != null && BCrypt.checkpw(s, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
