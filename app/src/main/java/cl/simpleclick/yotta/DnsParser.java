package cl.simpleclick.yotta;

// DnsParser.java (simplificado)


public class DnsParser {
    public static String extractDomain(byte[] dns) {
        if (dns.length < 12) return null;
        int offset = 12;
        StringBuilder name = new StringBuilder();
        while (offset < dns.length) {
            int len = dns[offset++] & 0xFF;
            if (len == 0) break;
            if (name.length() > 0) name.append('.');
            if (offset + len > dns.length) break;
            name.append(new String(dns, offset, len));
            offset += len;
        }
        return name.toString();
    }
}
