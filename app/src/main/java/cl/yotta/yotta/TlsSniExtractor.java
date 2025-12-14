package cl.yotta.yotta;


public class TlsSniExtractor {
    public static String extractSni(byte[] data, int length) {
        int pos = 0;

        if (!require(length, pos + 5) || (data[pos] & 0xFF) != 0x16) return null;
        pos += 5;

        if (!require(length, pos + 4) || (data[pos] & 0xFF) != 0x01) return null;
        pos += 4;

        pos += 2 + 32; // version + random

        if (!require(length, pos + 1)) return null;
        int sessionIdLength = data[pos] & 0xFF;
        pos += 1 + sessionIdLength;

        if (!require(length, pos + 2)) return null;
        int cipherSuitesLength = u16(data, pos);
        pos += 2 + cipherSuitesLength;

        if (!require(length, pos + 1)) return null;
        int compressionLength = data[pos] & 0xFF;
        pos += 1 + compressionLength;

        if (!require(length, pos + 2)) return null;
        int extensionsLength = u16(data, pos);
        pos += 2;
        int end = pos + extensionsLength;
        if (!require(length, end)) return null;

        while (pos + 4 <= end) {
            int extType = u16(data, pos);
            int extLen = u16(data, pos + 2);
            pos += 4;

            if (!require(length, pos + extLen)) return null;

            if (extType == 0x0000) { // SNI
                int sniListLen = u16(data, pos);
                pos += 2;

                int sniType = data[pos] & 0xFF;
                pos += 1;

                int hostLen = u16(data, pos);
                pos += 2;

                if (sniType != 0) return null;
                if (!require(length, pos + hostLen)) return null;

                return new String(data, pos, hostLen);
            } else {
                pos += extLen;
            }
        }
        return null;
    }

    private static int u16(byte[] data, int p) {
        return ((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF);
    }

    private static boolean require(int length, int needed) {
        return needed <= length;
    }
}
