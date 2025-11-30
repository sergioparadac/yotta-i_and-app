package cl.yotta;


import java.nio.ByteBuffer;

public class IPHeader {
    private final String sourceAddress;
    private final String destinationAddress;
    private final int headerLength;
    private final int protocol;
    private final int version;

    public IPHeader(ByteBuffer packet) {

        // 1. Leer Versión y Longitud de Encabezado (Byte 0)
        int versionAndHeaderLength = packet.get() & 0xFF;
        this.version = versionAndHeaderLength >> 4;
        this.headerLength = (versionAndHeaderLength & 0x0F) * 4;

        // 2. Saltar hasta el Protocolo (Byte 9)
        packet.position(9);
        this.protocol = packet.get() & 0xFF;

        // 3. Leer IP de Origen y Destino (Bytes 12-19)
        packet.position(12);
        this.sourceAddress = getAddress(packet);
        this.destinationAddress = getAddress(packet);

        // Reposicionar el buffer al inicio del encabezado de transporte
        packet.position(headerLength);
    }

    private String getAddress(ByteBuffer packet) {
        // Convierte los 4 bytes a un String de IP (Ej: "192.168.1.1")
        return (packet.get() & 0xFF) + "." + (packet.get() & 0xFF) + "." +
                (packet.get() & 0xFF) + "." + (packet.get() & 0xFF);
    }

    // ... Getters ...
    public int getVersion() { return version; }
    public int getHeaderLength() { return headerLength; }
    public int getProtocol() { return protocol; }
    public String getSourceAddress() { return sourceAddress; }
    public String getDestinationAddress() { return destinationAddress; }
}
