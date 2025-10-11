package com.example.customkeyboardsystemlevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences; // Importación añadida
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.customkeyboardsystemlevel.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.StringJoiner; // Importación añadida

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    public static final String SHARED_PREFS_NAME = "CustomKeyboardPrefs"; // Constante añadida
    public static final String DATA_LIST_KEY = "dataList"; // Constante añadida

    // Variables para RecyclerView y data
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private ArrayList<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataList = new ArrayList<>();
        dataList.add(1);
        saveDataListToPrefs(); // << MODIFICACIÓN: Guardar la lista inicial

        recyclerView = binding.recyclerViewList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new MyAdapter(dataList);
        recyclerView.setAdapter(adapter);

        binding.buttonAddItem.setOnClickListener(v -> addItem());
        binding.buttonRemoveItem.setOnClickListener(v -> removeItem());

        checkOverlayPermission();
    }

    // Método para añadir un ítem a la lista
    private void addItem() {
        int newItem = dataList.isEmpty() ? 1 : dataList.get(dataList.size() - 1) + 1;
        int position = dataList.size();
        dataList.add(newItem);
        adapter.notifyItemInserted(position);
        recyclerView.scrollToPosition(position);
        saveDataListToPrefs(); // << MODIFICACIÓN: Guardar lista tras añadir
    }

    // Método para eliminar el último ítem de la lista
    private void removeItem() {
        if (!dataList.isEmpty()) {
            int position = dataList.size() - 1;
            dataList.remove(position);
            adapter.notifyItemRemoved(position);
            saveDataListToPrefs(); // << MODIFICACIÓN: Guardar lista tras eliminar
        } else {
            Toast.makeText(this, "La lista está vacía.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * MODIFICACIÓN: Nuevo método para guardar la lista de datos en SharedPreferences.
     * Convierte la lista de enteros en un único string separado por comas.
     */
    private void saveDataListToPrefs() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Usamos StringJoiner para crear un string separado por comas (ej: "1,2,3")
        StringJoiner joiner = new StringJoiner(",");
        for (Integer item : dataList) {
            joiner.add(String.valueOf(item));
        }
        
        editor.putString(DATA_LIST_KEY, joiner.toString());
        editor.apply();
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
