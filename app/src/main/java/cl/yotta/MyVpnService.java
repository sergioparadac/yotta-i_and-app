package cl.yotta;


import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class MyVpnService extends VpnService implements Runnable {

    private static final String TAG = "SNIVpnService";
    private static final int PACKET_SIZE = 65535;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread;
    private FileOutputStream vpnOutputStream;

    // Gestores de Conexión
    private final ConcurrentHashMap<String, Connection> tcpConnectionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UDPConnection> udpConnectionMap = new ConcurrentHashMap<>();

    // --- CICLO DE VIDA Y CONFIGURACIÓN ---

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ... (Verificación e inicio de hilo)
        setupVpnInterface();
        if (vpnInterface != null) {
            vpnThread = new Thread(this, "VpnThread");
            vpnThread.start();
        }
        return START_STICKY;
    }

    private void setupVpnInterface() {
        if (vpnInterface == null) {
            Builder builder = new Builder();
            builder.setSession("SNI Local Analyzer")
                    .addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0);
            try {
                vpnInterface = builder.establish();
                Log.i(TAG, "Interfaz VPN local establecida.");
            } catch (Exception e) {
                Log.e(TAG, "Error al establecer la interfaz VPN", e);
            }
        }
    }

    // --- BUCLE DE LECTURA ---

    @Override
    public void run() {
        ByteBuffer packet = ByteBuffer.allocate(PACKET_SIZE);
        try (FileInputStream input = new FileInputStream(vpnInterface.getFileDescriptor())) {

            // Output Stream para reescribir respuestas (UDP) o paquetes no analizados (ICMP)
            vpnOutputStream = new FileOutputStream(vpnInterface.getFileDescriptor());

            while (!Thread.interrupted()) {
                int length = input.read(packet.array());
                if (length > 0) {
                    packet.limit(length);
                    packet.position(0);
                    // Llama al manejador que delega el análisis y reenvío
                    handlePacket(packet, vpnOutputStream);
                    packet.clear();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Hilo de lectura del túnel VPN finalizado", e);
        } finally {
            closeVpnInterface();
        }
    }

    // ... (Métodos onDestroy, onRevoke y closeVpnInterface - deben cerrar recursos) ...

    // --- MANEJO DE PAQUETES DE RED ---

    private void handlePacket(ByteBuffer packet, FileOutputStream outputStream) throws IOException {

        // 1. Análisis de Capa IP
        packet.position(0);
        IPHeader ipHeader = new IPHeader(packet);

        if (ipHeader.getVersion() != 4) {
            outputStream.write(packet.array(), 0, packet.limit()); // Reenvío simple si no es IPv4
            return;
        }

        int transportHeaderStart = ipHeader.getHeaderLength();

        switch (ipHeader.getProtocol()) {
            case 6: // TCP (Potencial SNI)
                handleTCPPacket(ipHeader, packet, transportHeaderStart);
                break;
            case 17: // UDP (DNS/Otros)
                handleUDPPacket(ipHeader, packet, transportHeaderStart, outputStream);
                break;
            default:
                // Tráfico no soportado (ICMP, etc.): Reenvío simple sin análisis
                outputStream.write(packet.array(), 0, packet.limit());
                break;
        }
    }

    // --- MANEJO TCP (ANÁLISIS SNI) ---
    private void handleTCPPacket(IPHeader ipHeader, ByteBuffer packet, int tcpHeaderStart) throws IOException {

        TCPHeader tcpHeader = new TCPHeader(packet, tcpHeaderStart);
        String connectionKey = ipHeader.getSourceAddress() + ":" + tcpHeader.getSourcePort()
                + "->" + ipHeader.getDestinationAddress() + ":" + tcpHeader.getDestinationPort();

        Connection connection = tcpConnectionMap.get(connectionKey);

        if (connection == null) {
            if (tcpHeader.isSYN()) {
                connection = new Connection(ipHeader, tcpHeader, this, connectionKey);
                tcpConnectionMap.put(connectionKey, connection);
            } else {
                return;
            }
        }

        // Mover al inicio del payload TCP
        int payloadStart = tcpHeaderStart + tcpHeader.getHeaderLength();
        packet.position(payloadStart);

        // 2. EXTRACCIÓN SNI
        if (!connection.isSNIExtracted() && tcpHeader.hasPayload()) {
            int payloadLength = packet.limit() - payloadStart;

            // *** LÓGICA CLAVE DE SNIExtractor (DEBE SER IMPLEMENTADA) ***
            String sni = SNIExtractor.extract(packet, payloadStart, payloadLength);

            if (sni != null) {
                Log.i(TAG, "⭐ SNI CAPTURADO: " + sni);
                connection.setSNIExtracted();
                sendSNIToUI(sni); // Envía a la MainActivity
            }

            packet.position(payloadStart); // Resetear posición para el reenvío
        }

        // 3. REENVÍO TRANSPARENTE
        // El método sendToNetwork en Connection usa un SocketChannel protegido para reenviar
        // el paquete a Internet sin interrumpir el flujo.
        connection.sendToNetwork(packet);

        if (tcpHeader.isFIN() || tcpHeader.isRST()) {
            connection.close();
            tcpConnectionMap.remove(connectionKey);
        }
    }

    // Dentro de MyVpnService.java

    private void handleUDPPacket(IPHeader ipHeader, ByteBuffer packet, int udpHeaderStart, FileOutputStream outputStream) throws IOException {

        // 1. Lectura de puertos UDP
        packet.position(udpHeaderStart);
        int srcPort = packet.getShort() & 0xFFFF; // Puerto de origen del cliente (srcPort)
        int dstPort = packet.getShort() & 0xFFFF; // Puerto de destino del servidor (dstPort)

        String clientAddress = ipHeader.getSourceAddress();
        String serverAddress = ipHeader.getDestinationAddress();

        String connectionKey = clientAddress + ":" + srcPort + "->" + serverAddress + ":" + dstPort;

        UDPConnection connection = udpConnectionMap.get(connectionKey);

        if (connection == null) {
            try {
                // **LA LLAMADA AL CONSTRUCTOR**
                connection = new UDPConnection(this, outputStream,
                        clientAddress, srcPort,
                        serverAddress, dstPort);
                udpConnectionMap.put(connectionKey, connection);
            } catch (IOException e) {
                Log.e(TAG, "Fallo al crear conexión UDP", e);
                return;
            }
        }

        // ... (Reenvío del payload)
        packet.position(udpHeaderStart + 8);
        connection.sendToNetwork(packet);
    }
    // --- COMUNICACIÓN CON LA UI ---
    private void sendSNIToUI(String sni) {
        Intent intent = new Intent("SNI_CAPTURED_EVENT");
        intent.putExtra("SNI_VALUE", sni);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Dentro de la clase MyVpnService.java

// ...

    /**
     * Cierra la interfaz VPN y todos los recursos asociados (conexiones TCP/UDP).
     */
    private void closeVpnInterface() {
        Log.i(TAG, "Cerrando interfaz VPN y limpiando conexiones...");

        // 1. Interrumpir el hilo de lectura de paquetes
        if (vpnThread != null) {
            vpnThread.interrupt();
        }

        // 2. Cerrar y limpiar todas las conexiones TCP activas
        for (Connection conn : tcpConnectionMap.values()) {
            conn.close();
        }
        tcpConnectionMap.clear();

        // 3. Cerrar y limpiar todos los canales UDP activos
        for (UDPConnection conn : udpConnectionMap.values()) {
            conn.close();
        }
        udpConnectionMap.clear();

        // 4. Cerrar el FileOutputStream (usado para escribir respuestas)
        if (vpnOutputStream != null) {
            try {
                vpnOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar FileOutputStream", e);
            }
        }

        // 5. Cerrar la interfaz TUN (ParcelFileDescriptor)
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                vpnInterface = null;
                Log.i(TAG, "Interfaz VPN cerrada con éxito.");
            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar ParcelFileDescriptor", e);
            }
        }
    }
}
