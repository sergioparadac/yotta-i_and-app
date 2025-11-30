package cl.yotta;


import java.nio.ByteBuffer;

public class TCPHeader {
    private final int sourcePort;
    private final int destinationPort;
    private final int headerLength;
    private final boolean syn, ack, fin, rst; // Flags esenciales

    public TCPHeader(ByteBuffer packet, int startPosition) {

        // *** IMPLEMENTAR LECTURA BINARIA ***

        packet.position(startPosition);
        this.sourcePort = packet.getShort() & 0xFFFF; // Puertos (2 bytes)
        this.destinationPort = packet.getShort() & 0xFFFF;

        // Saltar Números de Secuencia y Acuse (8 bytes)
        packet.position(startPosition + 12);

        // Flags y Longitud (Byte 12 y 13)
        int flagsAndOffset = packet.getShort() & 0xFFFF;

        this.headerLength = ((flagsAndOffset >> 12) & 0x0F) * 4; // Los 4 bits más significativos

        int flags = flagsAndOffset & 0x1FF; // Los 9 bits menos significativos
        this.syn = (flags & 0x0002) != 0;
        this.ack = (flags & 0x0010) != 0;
        this.fin = (flags & 0x0001) != 0;
        this.rst = (flags & 0x0004) != 0;

        // Reposicionar el buffer al inicio del encabezado TCP
        packet.position(startPosition + headerLength);
    }

    // ... Getters ...
    public int getSourcePort() { return sourcePort; }
    public int getDestinationPort() { return destinationPort; }
    public int getHeaderLength() { return headerLength; }
    public boolean isSYN() { return syn; }
    public boolean isACK() { return ack; }
    public boolean isFIN() { return fin; }
    public boolean isRST() { return rst; }
    public boolean hasPayload() {
        // Si no es SYN, FIN, RST, ACK (y tiene longitud mayor a 0, implícito por el llamador)
        // se asume que lleva datos.
        return !(syn || fin || rst) && ack;
    }
}