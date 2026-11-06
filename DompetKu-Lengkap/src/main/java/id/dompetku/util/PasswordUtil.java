package id.dompetku.util;
import org.mindrot.jbcrypt.BCrypt;
/** Hash password dengan BCrypt; jangan pernah simpan password plaintext. */
public final class PasswordUtil {
 private PasswordUtil(){}
 public static String hash(String s){if(s==null||s.length()<8)throw new IllegalArgumentException("Password minimal 8 karakter.");return BCrypt.hashpw(s,BCrypt.gensalt(12));}
 public static boolean verify(String s,String hash){try{return s!=null&&hash!=null&&BCrypt.checkpw(s,hash);}catch(IllegalArgumentException e){return false;}}
}
