package cl.simpleclick.yotta;

import android.net.VpnService;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class Forwarder {

    private final VpnService vpnService;

    public Forwarder(VpnService svc) {
        this.vpnService = svc;
    }

    public void forwardTcp(String dstIp, int dstPort, InputStream clientIn, OutputStream clientOut) {
        try {
            Socket socket = new Socket();
            vpnService.protect(socket); // proteger socket
            socket.connect(new InetSocketAddress(dstIp, dstPort));

            // Hilo para enviar datos al servidor
            new Thread(() -> {
                try (OutputStream out = socket.getOutputStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = clientIn.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        out.flush();
                    }
                } catch (Exception ignored) {}
            }).start();

            // Hilo para recibir datos del servidor y devolver al cliente
            new Thread(() -> {
                try (InputStream in = socket.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        clientOut.write(buf, 0, len);
                        clientOut.flush();
                    }
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void forwardUdp(String dstIp, int dstPort, DatagramSocket clientSocket) {
        try {
            DatagramSocket socket = new DatagramSocket();
            vpnService.protect(socket); // proteger socket

            new Thread(() -> {
                try {
                    byte[] buf = new byte[4096];
                    while (true) {
                        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                        clientSocket.receive(pkt);
                        DatagramPacket outPkt = new DatagramPacket(pkt.getData(), pkt.getLength(),
                                InetAddress.getByName(dstIp), dstPort);
                        socket.send(outPkt);
                    }
                } catch (Exception ignored) {}
            }).start();

            new Thread(() -> {
                try {
                    byte[] buf = new byte[4096];
                    while (true) {
                        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                        socket.receive(pkt);
                        clientSocket.send(pkt);
                    }
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
