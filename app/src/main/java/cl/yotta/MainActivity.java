package cl.yotta;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int VPN_REQUEST_CODE = 1001;

    private TextView statusTextView;
    private TextView sniLogTextView;
    private Button startButton;
    private Button stopButton;

    // BroadcastReceiver para recibir mensajes de SNI capturado del VpnService
    private BroadcastReceiver sniReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sni = intent.getStringExtra("SNI_VALUE");
            if (sni != null) {
                // Agregar el SNI al log en la UI
                String currentLog = sniLogTextView.getText().toString();
                sniLogTextView.setText(sni + "\n" + currentLog);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        sniLogTextView = findViewById(R.id.sniLogTextView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        // Registrar el receptor para la comunicación local del servicio
        LocalBroadcastManager.getInstance(this).registerReceiver(
                sniReceiver, new IntentFilter("SNI_CAPTURED_EVENT"));

        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar el receptor
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sniReceiver);
    }

    // Método llamado por el botón 'Iniciar' en el XML
    public void startVpn(View view) {
        // 1. Verificar si el permiso de VPN está otorgado
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            // Permiso no otorgado, solicitar al usuario
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            // Permiso ya otorgado, iniciar el servicio
            startVpnService();
        }
    }

    // Método llamado por el botón 'Detener' en el XML
    public void stopVpn(View view) {
        // Enviar un Intent para detener el servicio VPN
        Intent intent = new Intent(this, MyVpnService.class);
        stopService(intent);
        updateUI();
        Toast.makeText(this, "VPN Detenida.", Toast.LENGTH_SHORT).show();
    }



    private void startVpnService() {
        Intent intent = new Intent(this, MyVpnService.class);
        startService(intent);
        updateUI();
        Toast.makeText(this, "VPN Iniciada. Analizando tráfico...", Toast.LENGTH_LONG).show();
    }

    // Actualiza el estado de los botones y el texto
    private void updateUI() {
        // Comprobar si el servicio VPN está activo (requiere un método en el VpnService real)
        // Por simplicidad, se comprueba si hay una VPN activa globalmente (menos preciso)
        boolean isVpnActive = VpnService.prepare(this) == null;

        if (isVpnActive) {
            statusTextView.setText("Estado: ACTIVO");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusTextView.setText("Estado: Desconectado");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Es crucial llamar a super.onActivityResult()
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Permiso otorgado. Continuar con la acción original: iniciar el servicio.
                Log.i(TAG, "Permiso de VPN otorgado.");
                startVpnService();
            } else {
                // Permiso denegado. Informar al usuario.
                Log.w(TAG, "Permiso de VPN denegado por el usuario.");
                Toast.makeText(this, "Permiso de VPN denegado. No se puede iniciar el análisis.", Toast.LENGTH_LONG).show();
            }
        }
    }

// ... (resto de la clase) ...
}