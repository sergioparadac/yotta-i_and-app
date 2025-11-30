package cl.yotta;


import android.net.VpnService;
import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public  class Connection {
    private final String TAG = "VPN_TCP";
    private final VpnService vpnService;
    private final String connectionKey;
    private final String destinationAddress;
    private final int destinationPort;
    private SocketChannel remoteChannel;
    private boolean isSNIExtracted = false;

    public Connection(IPHeader ipHeader, TCPHeader tcpHeader, VpnService service, String key) {
        this.vpnService = service;
        this.connectionKey = key;
        this.destinationAddress = ipHeader.getDestinationAddress();
        this.destinationPort = tcpHeader.getDestinationPort();
        connectRemote();
    }

    private void connectRemote() {
        try {
            remoteChannel = SocketChannel.open();
            vpnService.protect(remoteChannel.socket());
            remoteChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
            remoteChannel.configureBlocking(false);
        } catch (IOException e) {
            Log.e(TAG, "Error al conectar o proteger el socket: " + connectionKey, e);
            close();
        }
    }

    public void sendToNetwork(ByteBuffer packet) throws IOException {
        if (remoteChannel != null && remoteChannel.isConnected() && packet.hasRemaining()) {
            remoteChannel.write(packet);
        }
    }

    public boolean isSNIExtracted() {
        return isSNIExtracted;
    }

    public void setSNIExtracted() {
        this.isSNIExtracted = true;
    }

    public void close() {
        try {
            if (remoteChannel != null) remoteChannel.close();
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar la conexión: " + connectionKey, e);
        }
    }
}