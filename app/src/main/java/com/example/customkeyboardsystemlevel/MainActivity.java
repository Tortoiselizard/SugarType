package com.example.customkeyboardsystemlevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.customkeyboardsystemlevel.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    // Variables para RecyclerView y data
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private ArrayList<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // set content view to binding's root
        setContentView(binding.getRoot());

        // 1. Inicializar la lista de datos con el ítem inicial '1'
        dataList = new ArrayList<>();
        dataList.add(1);

        // 2. Configurar el RecyclerView
        recyclerView = binding.recyclerViewList;
        // El LayoutManager es necesario para posicionar los ítems (lista vertical)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 3. Crear e inicializar el Adapter
        adapter = new MyAdapter(dataList);
        recyclerView.setAdapter(adapter);

        // 4. Configurar los Listeners de los botones
        binding.buttonAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        binding.buttonRemoveItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeItem();
            }
        });

        // Llamada para verificar el permiso de superposición (flotante)
        checkOverlayPermission();
    }

    // Método para añadir un ítem a la lista
    private void addItem() {
        // El nuevo ítem será el número siguiente al último elemento
        int newItem = dataList.isEmpty() ? 1 : dataList.get(dataList.size() - 1) + 1;
        int position = dataList.size(); // La posición de inserción es al final

        // 1. Añadir el nuevo dato al ArrayList
        dataList.add(newItem);

        // 2. Notificar al Adapter que se ha insertado un ítem
        // Esto garantiza una actualización eficiente y con animaciones
        adapter.notifyItemInserted(position);
        
        // Opcional: Desplazarse al final de la lista para ver el nuevo ítem
        recyclerView.scrollToPosition(position);
    }

    // Método para eliminar el último ítem de la lista
    private void removeItem() {
        if (!dataList.isEmpty()) {
            int position = dataList.size() - 1; // La posición a eliminar es el último

            // 1. Eliminar el dato del ArrayList
            dataList.remove(position);

            // 2. Notificar al Adapter que se ha eliminado un ítem
            // Esto garantiza una actualización eficiente y con animaciones
            adapter.notifyItemRemoved(position);
        } else {
            Toast.makeText(this, "La lista está vacía.", Toast.LENGTH_SHORT).show();
        }
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
                    // Permiso concedido
                    Toast.makeText(this, "Permiso concedido para superponer apps.", Toast.LENGTH_SHORT).show();
                } else {
                    // Permiso denegado
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
