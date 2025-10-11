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
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    public static final String SHARED_PREFS_NAME = "CustomKeyboardPrefs";
    public static final String DATA_LIST_KEY = "dataList";
    // Constante para la acción del Broadcast
    public static final String ACTION_DATA_UPDATE = "com.example.customkeyboardsystemlevel.DATA_UPDATE";

    // Variables para RecyclerView y data
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private ArrayList<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // MODIFICACIÓN: Cargar o inicializar la lista
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
        
        // AÑADIDO: Listener para el nuevo botón "Edit"
        binding.buttonEditItem.setOnClickListener(v -> showEditPopup());

        // MODIFICACIÓN: Enviar el broadcast inicial al crear la actividad
        sendDataListBroadcast();

        checkOverlayPermission();
    }
    
    // MODIFICACIÓN: Nuevo método para cargar la lista desde SharedPreferences
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

    // Método para añadir un ítem a la lista
    private void addItem() {
        int newItem = dataList.isEmpty() ? 1 : dataList.get(dataList.size() - 1) + 1;
        int position = dataList.size();
        dataList.add(newItem);
        adapter.notifyItemInserted(position);
        recyclerView.scrollToPosition(position);
        saveDataListToPrefs();
        sendDataListBroadcast(); // << AÑADIDO: Enviar broadcast tras añadir
    }

    // Método para eliminar el último ítem de la lista
    private void removeItem() {
        if (!dataList.isEmpty()) {
            int position = dataList.size() - 1;
            dataList.remove(position);
            adapter.notifyItemRemoved(position);
            saveDataListToPrefs();
            sendDataListBroadcast(); // << AÑADIDO: Enviar broadcast tras eliminar
        } else {
            Toast.makeText(this, "La lista está vacía.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * AÑADIDO: Método para mostrar el popup "Editing".
     */
    private void showEditPopup() {
        Toast.makeText(this, "Editing", Toast.LENGTH_SHORT).show();
    }


    /**
     * Nuevo método para guardar la lista de datos en SharedPreferences.
     */
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

    /**
     * AÑADIDO: Nuevo método para enviar un broadcast local con la lista de datos.
     */
    private void sendDataListBroadcast() {
        Intent intent = new Intent(ACTION_DATA_UPDATE);
        // Usamos una copia de la lista para evitar problemas de concurrencia/mutabilidad
        intent.putIntegerArrayListExtra(DATA_LIST_KEY, new ArrayList<>(dataList));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // El resto de los métodos (checkOverlayPermission, onActivityResult, onDestroy) permanecen sin cambios...
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
