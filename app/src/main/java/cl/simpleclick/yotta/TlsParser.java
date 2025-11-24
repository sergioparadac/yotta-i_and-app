package cl.simpleclick.yotta;

// TlsParser.java (simplificado)


import java.util.Arrays;

public class TlsParser {
    public static String extractSni(byte[] tls) {
        if (tls.length < 5 || tls[0] != 22) return null;
        int pos = 43; // saltar versión+random+sessionId
        if (pos >= tls.length) return null;
        // búsqueda simplificada de extensión SNI
        for (int i = pos; i < tls.length - 3; i++) {
            if (tls[i] == 0x00 && tls[i+1] == 0x00) {
                int nameLen = ((tls[i+2] & 0xFF) << 8) | (tls[i+3] & 0xFF);
                if (i+4+nameLen <= tls.length) {
                    return new String(Arrays.copyOfRange(tls, i+4, i+4+nameLen));
                }
            }
        }
        return null;
    }
}
