package cl.yotta.yotta;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.InetAddresses;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SniAdapter adapter;
    private final List<String> sniList = new ArrayList<>();

    private ActivityResultLauncher<Intent> vpnLauncher;

    private final BroadcastReceiver sniReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sni = intent.getStringExtra("sni");
            if (sni != null && !sni.isEmpty()) {
                sniList.add(sni);
                adapter.notifyItemInserted(sniList.size() - 1);
                recyclerView.scrollToPosition(sniList.size() - 1);
            }
        }
    };

    private final ActivityResultLauncher<Intent> vpnPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startVpnService();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.reclListaMain);
        adapter = new SniAdapter(sniList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Registrar el BroadcastReceiver con compatibilidad SDK 33+
        IntentFilter filter = new IntentFilter("SNI_CAPTURED");
        registerReceiver(sniReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        Button buttonGrabar = findViewById(R.id.btnGrabarMain);

        buttonGrabar.setOnClickListener(v -> {
            Intent prep = VpnService.prepare(this);
            if (prep != null) {
                vpnPermissionLauncher.launch(prep);
            } else {
                startVpnService();
            }
        });


    }

    private void startVpnService() {
        startService(new Intent(this, MyVpnService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sniReceiver);
    }

}