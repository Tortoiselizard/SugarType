package com.example.customkeyboardsystemlevel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// El Adapter se encarga de crear ViewHolders y vincular los datos a ellos.
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private ArrayList<Integer> mDataset;
    // NUEVA CONSTANTE para la clave de SharedPreferences del texto
    public static final String TEXT_KEY_PREFIX = "item_text_";
    
    // NUEVA CONSTANTE para la acción de Broadcast cuando se actualiza el texto
    public static final String ACTION_TEXT_UPDATE = "com.example.customkeyboardsystemlevel.TEXT_UPDATE";
    public static final String UPDATED_BUTTON_VALUE_KEY = "updatedButtonValue"; // Clave para el número de ítem

    // Constructor que acepta el conjunto de datos de números.
    public MyAdapter(ArrayList<Integer> myDataset) {
        mDataset = myDataset;
    }

    // 1. ViewHolder: Proporciona una referencia a las vistas para cada elemento de datos.
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public EditText editText;
        // NUEVO: Listener para guardar el texto
        public TextWatcher textWatcher;

        public MyViewHolder(View v) {
            super(v);
            // Referencia al TextView del list_item.xml
            textView = v.findViewById(R.id.textViewItemNumber);
            // **Nuevo:** Referencia al EditText del list_item.xml
            editText = v.findViewById(R.id.editTextItemInput);
        }
    }

    // 2. onCreateViewHolder: Crea nuevas vistas (infla el layout del ítem).
    @NonNull
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Crea una nueva vista a partir de list_item.xml
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(v);
    }

    // 3. onBindViewHolder: Reemplaza el contenido de la vista con el dato en la posición dada.
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final Context context = holder.itemView.getContext();
        final int itemNumber = mDataset.get(position);
        final String itemNumberStr = String.valueOf(itemNumber);
        
        // Obtiene el número de la posición y lo establece en el TextView
        holder.textView.setText(itemNumberStr);
        
        // 1. Cargar el texto guardado para este ítem
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String savedText = prefs.getString(TEXT_KEY_PREFIX + itemNumberStr, "");
        
        // 2. Remover el listener anterior para evitar que se dispare al establecer el texto
        if (holder.textWatcher != null) {
            holder.editText.removeTextChangedListener(holder.textWatcher);
        }
        
        // 3. Establecer el texto cargado
        holder.editText.setText(savedText);

        // 4. Crear y asignar un nuevo listener de texto
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se necesita
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se necesita
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Guardar el texto en SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(TEXT_KEY_PREFIX + itemNumberStr, s.toString());
                editor.apply();
                
                // *** NUEVA MODIFICACIÓN: Enviar Broadcast Local con el número de ítem actualizado ***
                Intent intent = new Intent(ACTION_TEXT_UPDATE);
                intent.putExtra(UPDATED_BUTTON_VALUE_KEY, itemNumberStr);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                // *********************************************************************************
            }
        };
        holder.editText.addTextChangedListener(holder.textWatcher);
    }

    // 4. getItemCount: Devuelve el tamaño del dataset.
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
