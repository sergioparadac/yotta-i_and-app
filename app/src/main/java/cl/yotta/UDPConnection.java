package cl.yotta;


import android.net.VpnService;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


public class UDPConnection {


    private static final String TAG = "UDP_Connection";
    private static final int PACKET_SIZE = 65535;
    
    private final VpnService vpnService;

    // El stream para escribir respuestas de vuelta al túnel
    private final FileOutputStream vpnOutputStream;

    // El canal de comunicación real hacia Internet
    private final DatagramChannel remoteChannel;

    // El hilo dedicado a leer respuestas del servidor
    private final Thread readThread;

    // Almacenamiento de direcciones originales para simular la respuesta del servidor
    private final String clientAddress;
    private final int clientPort;
    private final String serverAddress;
    private final int serverPort;


    //private final DatagramChannel remoteChannel;


    public UDPConnection(VpnService service, FileOutputStream outputStream,
                         String clientAddress, int clientPort,
                         String serverAddress, int serverPort) throws IOException {

        this.vpnService = service;
        this.vpnOutputStream = outputStream;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        remoteChannel = DatagramChannel.open();
        service.protect(remoteChannel.socket());
        remoteChannel.connect(new InetSocketAddress(serverAddress, serverPort));
        remoteChannel.configureBlocking(false);

        Log.d(TAG, "Canal UDP abierto: " + clientAddress + ":" + clientPort + " -> " + serverAddress + ":" + serverPort);

        // Iniciar el hilo de lectura para esperar la respuesta del servidor
        readThread = new Thread((Runnable) this, "UDPReadThread-" + serverPort);
        readThread.start();
    }


    public void sendToNetwork(ByteBuffer packet) throws IOException {
        if (remoteChannel != null && remoteChannel.isConnected()) {
            // Write escribirá los bytes restantes desde la posición actual del buffer.
            remoteChannel.write(packet);
        }
    }

    public void close() {
        try {
            if (remoteChannel != null) {
                remoteChannel.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar el canal UDP", e);
        }
    }
}