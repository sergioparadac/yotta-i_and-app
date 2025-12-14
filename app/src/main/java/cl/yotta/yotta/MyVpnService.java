package cl.yotta.yotta;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import java.io.FileInputStream;
import java.io.IOException;

public class MyVpnService extends VpnService implements Runnable {
    private Thread thread;
    private ParcelFileDescriptor vpnInterface;

    private static final String CHANNEL_ID = "vpn_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_STOP = "STOP_VPN";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Crear canal de notificación
        createNotificationChannel();


        PendingIntent stopPendingIntent = null;

        // Intent para detener el servicio desde la notificación
        Intent stopIntent = new Intent(this, MyVpnService.class);
        stopIntent.setAction(ACTION_STOP);
        stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notificación foreground


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VPN activa")
                .setContentText("Capturando tráfico y extrayendo SNI")
                .setSmallIcon(android.R.drawable.ic_lock_lock) // ícono válido
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detente VPN", stopPendingIntent)
                .build();

        // IMPORTANTE: llamar inmediatamente al iniciar
        startForeground(NOTIFICATION_ID, notification);

        // Iniciar hilo de captura
        stopWorker();
        thread = new Thread(this, "MyVpnService");
        thread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopWorker();
        super.onDestroy();
    }

    private void stopWorker() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) {}
            vpnInterface = null;
        }
    }

    @Override
    public void run() {
        Builder builder = new Builder();
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);

        vpnInterface = builder.establish();
        if (vpnInterface == null) {
            stopSelf();
            return;
        }

        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        byte[] packet = new byte[32767];

        while (!Thread.interrupted()) {
            try {
                int length = in.read(packet);
                if (length > 0) {
                    String sni = TlsSniExtractor.extractSni(packet, length);
                    if (sni != null && !sni.isEmpty()) {
                        Intent sniIntent = new Intent("SNI_CAPTURED");
                        sniIntent.setPackage(getPackageName()); // limitar a tu app
                        sniIntent.putExtra("sni", sni);
                        sendBroadcast(sniIntent);
                    }
                }
            } catch (IOException e) {
                break;
            }
        }

        stopWorker();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Canal para servicio VPN");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
