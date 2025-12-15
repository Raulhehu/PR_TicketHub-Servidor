package pr.tickethub.servidor.bd;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class Seguridad {

    // metodo estatico para no tener que instanciar la clase, solo lo llamo y ya
    public static String encriptar(String password) {
        try {
            // selecciono el algoritmo SHA-256 que es bastante seguro para esto
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // convierto la contraseña a bytes y le aplico el hash
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // ahora tengo que convertir esos bytes raros a texto hexadecimal legible
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // regreso la cadena ya encriptada
            return hexString.toString();
            
        } catch (Exception ex) {
            // si falla algo (raro que pase), imprimo el error
            ex.printStackTrace();
            return null;
        }
    }
}
