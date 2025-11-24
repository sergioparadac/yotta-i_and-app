package cl.simpleclick.yotta;

// MyVpnService.java



import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;



import androidx.core.app.NotificationCompat;



public class MyVpnService extends VpnService {

    private static final String CHANNEL_ID = "vpn_channel";
    private ParcelFileDescriptor tunFd;
    private Thread readerThread;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Crear canal y notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "vpn_channel",
                    "VPN Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }


        Notification notification = new NotificationCompat.Builder(this, "vpn_channel")
                .setContentTitle("VPN activa")
                .setContentText("Capturando dominios en tiempo real")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .build();

        // 👉 Aquí ya no debería dar error
        startForeground(1, notification);

        // Configuración del túnel
        Builder builder = new Builder();
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer("1.1.1.1");
        tunFd = builder.establish();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (tunFd != null) tunFd.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}