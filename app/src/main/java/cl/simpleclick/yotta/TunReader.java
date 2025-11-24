package cl.simpleclick.yotta;

// TunReader.java

import java.io.FileInputStream;
import java.util.Arrays;

public class TunReader implements Runnable {
    private final FileInputStream in;
    private final DomainReporter reporter;
    private final Forwarder forwarder;

    public TunReader(FileInputStream in, DomainReporter reporter, Forwarder forwarder) {
        this.in = in;
        this.reporter = reporter;
        this.forwarder = forwarder;
    }

    @Override
    public void run() {
        byte[] buf = new byte[1500];
        try {
            while (true) {
                int len = in.read(buf);
                if (len > 0) parsePacket(buf, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parsePacket(byte[] packet, int len) {
        int version = (packet[0] >> 4) & 0xF;
        if (version != 4) return;
        int ihl = (packet[0] & 0x0F) * 4;
        int protocol = packet[9] & 0xFF;

        if (protocol == 17) { // UDP
            parseUdp(packet, ihl, len);
        } else if (protocol == 6) { // TCP
            parseTcp(packet, ihl, len);
        }
    }

    private void parseUdp(byte[] packet, int ihl, int len) {
        int udpOffset = ihl;
        int dstPort = ((packet[udpOffset + 2] & 0xFF) << 8) | (packet[udpOffset + 3] & 0xFF);
        if (dstPort == 53) {
            int payloadOffset = udpOffset + 8;
            byte[] dns = Arrays.copyOfRange(packet, payloadOffset, len);
            String domain = DnsParser.extractDomain(dns);
            if (domain != null && reporter != null) reporter.onDomainObserved(domain, DomainReporter.Source.DNS);
        }
        // 👉 Forwarding UDP (ejemplo simplificado)
        // forwarder.forwardUdp(dstIp, dstPort, clientSocket);
    }

    private void parseTcp(byte[] packet, int ihl, int len) {
        int tcpOffset = ihl;
        int dstPort = ((packet[tcpOffset + 2] & 0xFF) << 8) | (packet[tcpOffset + 3] & 0xFF);
        if (dstPort == 443) {
            int dataOffset = ((packet[tcpOffset + 12] >> 4) & 0xF) * 4;
            int payloadOffset = tcpOffset + dataOffset;
            byte[] tls = Arrays.copyOfRange(packet, payloadOffset, len);
            String sni = TlsParser.extractSni(tls);
            if (sni != null && reporter != null) reporter.onDomainObserved(sni, DomainReporter.Source.TLS_SNI);
        }
        // 👉 Forwarding TCP (ejemplo simplificado)
        // forwarder.forwardTcp(dstIp, dstPort, clientIn, clientOut);
    }
}