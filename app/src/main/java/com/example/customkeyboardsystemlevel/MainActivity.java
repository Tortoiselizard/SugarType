package com.example.customkeyboardsystemlevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.customkeyboardsystemlevel.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.StringJoiner;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    public static final String SHARED_PREFS_NAME = "CustomKeyboardPrefs";
    public static final String DATA_LIST_KEY = "dataList";
    // Constante para la acción del Broadcast
    public static final String ACTION_DATA_UPDATE = "com.example.customkeyboardsystemlevel.DATA_UPDATE";
    
    // Bandera para rastrear el estado del FloatingButtonService iniciado desde MainActivity
    // NOTA: Esta bandera no rastrea el estado si el servicio fue iniciado por CustomKeyboardApp
    private boolean isFloatingButtonServiceActive = false; 

    // Variables para RecyclerView y data
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private ArrayList<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Cargar o inicializar la lista
        loadDataListFromPrefs();
        if (dataList.isEmpty()) {
            dataList.add(1);
            saveDataListToPrefs();
        }

        recyclerView = binding.recyclerViewList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MyAdapter(dataList);
        recyclerView.setAdapter(adapter);

        binding.buttonAddItem.setOnClickListener(v -> addItem());
        binding.buttonRemoveItem.setOnClickListener(v -> removeItem());
        
        // Listener para el botón "Edit"
        binding.buttonEditItem.setOnClickListener(v -> showEditPopup());

        sendDataListBroadcast();
        checkOverlayPermission();
    }
    
    private void loadDataListFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        String savedList = prefs.getString(DATA_LIST_KEY, "");
        dataList = new ArrayList<>();

        if (savedList != null && !savedList.isEmpty()) {
            String[] items = savedList.split(",");
            for (String item : items) {
                try {
                    dataList.add(Integer.parseInt(item.trim()));
                } catch (NumberFormatException e) {
                    // Ignorar valores no numéricos
                }
            }
        }
    }

    private void addItem() {
        int newItem = dataList.isEmpty() ? 1 : dataList.get(dataList.size() - 1) + 1;
        int position = dataList.size();
        dataList.add(newItem);
        adapter.notifyItemInserted(position);
        recyclerView.scrollToPosition(position);
        saveDataListToPrefs();
        sendDataListBroadcast();
    }

    private void removeItem() {
        if (!dataList.isEmpty()) {
            int position = dataList.size() - 1;
            dataList.remove(position);
            adapter.notifyItemRemoved(position);
            saveDataListToPrefs();
            sendDataListBroadcast();
        } else {
            Toast.makeText(this, "La lista está vacía.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * MODIFICADO: Método para alternar (toggle) el FloatingButtonService.
     * 1. Detiene el servicio si está activo.
     * 2. Inicia el servicio si está inactivo.
     */
    private void showEditPopup() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        
        if (isFloatingButtonServiceActive) {
            // DETENER EL SERVICIO (Toggle OFF)
            stopService(serviceIntent);
            isFloatingButtonServiceActive = false;
            Toast.makeText(this, "Floating Button Service Stopped", Toast.LENGTH_SHORT).show();
            binding.buttonEditItem.setText("Edit (OFF)");

        } else {
            // 1. Verificar el permiso de superposición (draw over other apps)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // Si no tiene permiso, solicita el permiso y NO inicia el servicio
                checkOverlayPermission();
                return; 
            }
            
            // INICIAR EL SERVICIO (Toggle ON)
            
            // 2. Pasar la lista de datos actual del MainActivity al servicio
            serviceIntent.putIntegerArrayListExtra(DATA_LIST_KEY, new ArrayList<>(dataList));
            
            // 3. Iniciar el servicio
            startService(serviceIntent);
            isFloatingButtonServiceActive = true;
            Toast.makeText(this, "Floating Button Service Started", Toast.LENGTH_SHORT).show();
            binding.buttonEditItem.setText("Edit (ON)");
        }
    }


    private void saveDataListToPrefs() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringJoiner joiner = new StringJoiner(",");
        for (Integer item : dataList) {
            joiner.add(String.valueOf(item));
        }

        editor.putString(DATA_LIST_KEY, joiner.toString());
        editor.apply();
    }

    private void sendDataListBroadcast() {
        Intent intent = new Intent(ACTION_DATA_UPDATE);
        intent.putIntegerArrayListExtra(DATA_LIST_KEY, new ArrayList<>(dataList));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permiso concedido para superponer apps.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permiso denegado. El botón flotante no funcionará.", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
