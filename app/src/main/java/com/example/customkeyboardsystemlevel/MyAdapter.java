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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

// El Adapter se encarga de crear ViewHolders y vincular los datos a ellos.
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private ArrayList<Integer> mDataset;
    // NUEVA CONSTANTE para la clave de SharedPreferences del texto
    public static final String TEXT_KEY_PREFIX = "item_text_";
    
    // NUEVA CONSTANTE para la acción de Broadcast cuando se actualiza el texto
    public static final String ACTION_TEXT_UPDATE = "com.example.customkeyboardsystemlevel.TEXT_UPDATE";
    public static final String UPDATED_BUTTON_VALUE_KEY = "updatedButtonValue"; // Clave para el número de ítem

    // NUEVO: Mapa para rastrear el estado de colapso/expansión
    private HashMap<Integer, Boolean> expandedState = new HashMap<>();
    
    // Constante para el número de sub-items
    private static final int SUB_ITEM_COUNT = 8;

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
        
        // NUEVOS: Vistas para la funcionalidad de colapso
        public LinearLayout itemParentLayout;
        public LinearLayout sublistContainer;

        public MyViewHolder(View v) {
            super(v);
            // Referencia al TextView del list_item.xml
            textView = v.findViewById(R.id.textViewItemNumber);
            // Referencia al EditText del list_item.xml
            editText = v.findViewById(R.id.editTextItemInput);
            
            // Referencias a los nuevos layouts
            itemParentLayout = v.findViewById(R.id.itemParentLayout);
            sublistContainer = v.findViewById(R.id.sublistContainer);
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
        
        // --- 1. Configuración del Item Padre ---
        
        // Establecer el número de ítem
        holder.textView.setText(itemNumberStr);
        
        // Cargar y gestionar el texto del padre
        loadAndSetText(context, holder.editText, holder, itemNumberStr);

        // --- 2. Configuración de la Sublista (Colapso/Expansión) ---
        
        // 2a. Aplicar estado de expansión/colapso
        boolean isExpanded = expandedState.get(itemNumber) != null && expandedState.get(itemNumber);
        holder.sublistContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        
        // 2b. Listener para colapsar/expandir al hacer clic en la vista padre
        holder.itemParentLayout.setOnClickListener(v -> {
            boolean currentState = expandedState.get(itemNumber) != null && expandedState.get(itemNumber);
            boolean newState = !currentState;
            expandedState.put(itemNumber, newState);
            holder.sublistContainer.setVisibility(newState ? View.VISIBLE : View.GONE);
            // Notificar el cambio para asegurar que el RecyclerView se redibuje correctamente
            notifyItemChanged(position); 
        });
        
        // --- 3. Generación y Configuración Dinámica de Sub-Items ---
        
        // 3a. Limpiar el contenedor antes de añadir nuevos sub-items
        holder.sublistContainer.removeAllViews();
        
        // 3b. Generar los 8 sub-items
        for (int i = 1; i <= SUB_ITEM_COUNT; i++) {
            final String subItemNumberStr = itemNumberStr + "." + i;
            
            // Inflar el layout de un sub-ítem (reutilizaremos la estructura base de list_item para simplificar)
            // NOTA: Para un diseño más limpio, se debería crear un layout separado para el sub-ítem.
            View subItemView = LayoutInflater.from(context).inflate(R.layout.list_item, holder.sublistContainer, false);
            
            // Encontrar las vistas dentro del sub-ítem inflado
            TextView subTextView = subItemView.findViewById(R.id.textViewItemNumber);
            EditText subEditText = subItemView.findViewById(R.id.editTextItemInput);
            LinearLayout subParentLayout = subItemView.findViewById(R.id.itemParentLayout);
            LinearLayout subContainer = subItemView.findViewById(R.id.sublistContainer);
            
            // Ocultar el separador si es el último sub-item.
            if (i == SUB_ITEM_COUNT) {
                View separator = subItemView.findViewById(subItemView.getId() + 1); // Asumiendo que el separador es el siguiente View
                if (separator instanceof View && separator.getBackground() != null) {
                   // No se puede ocultar directamente, ya que el último <View> no tiene ID, lo dejamos visible.
                }
            }
            
            // El sub-ítem no debe ser clickeable para colapsar/expandir, y no tiene su propia sublista.
            subParentLayout.setOnClickListener(null);
            subContainer.setVisibility(View.GONE);
            
            // Establecer el número del sub-ítem
            subTextView.setText(subItemNumberStr);
            
            // Cargar y gestionar el texto del sub-ítem. Usamos un TextWatcher local.
            // Para evitar la complejidad de crear un Holder para el Sub-Item, creamos el TextWatcher aquí.
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            String savedText = prefs.getString(TEXT_KEY_PREFIX + subItemNumberStr, "");
            subEditText.setText(savedText);
            
            // Remover y crear un nuevo TextWatcher para el Sub-Item
            TextWatcher subTextWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // Guardar el texto en SharedPreferences con la clave del sub-ítem
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(TEXT_KEY_PREFIX + subItemNumberStr, s.toString());
                    editor.apply();
                    
                    // Enviar Broadcast Local con el número de sub-ítem actualizado
                    Intent intent = new Intent(ACTION_TEXT_UPDATE);
                    intent.putExtra(UPDATED_BUTTON_VALUE_KEY, subItemNumberStr);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            };
            
            // El subItemView contiene el layout del padre. 
            // Necesitamos acceder al EditText específico del sub-ítem.
            subEditText.addTextChangedListener(subTextWatcher);
            
            // Añadir el sub-ítem al contenedor
            holder.sublistContainer.addView(subItemView);
        }
    }
    
    /**
     * Método auxiliar para cargar el texto, aplicarlo y configurar el TextWatcher para el item padre.
     */
    private void loadAndSetText(Context context, EditText editText, MyViewHolder holder, String itemNumberStr) {
        // 1. Cargar el texto guardado
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String savedText = prefs.getString(TEXT_KEY_PREFIX + itemNumberStr, "");
        
        // 2. Remover el listener anterior
        if (holder.textWatcher != null) {
            editText.removeTextChangedListener(holder.textWatcher);
        }
        
        // 3. Establecer el texto cargado
        editText.setText(savedText);

        // 4. Crear y asignar un nuevo listener de texto para el item padre
        holder.textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Guardar el texto en SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(TEXT_KEY_PREFIX + itemNumberStr, s.toString());
                editor.apply();
                
                // Enviar Broadcast Local con el número de ítem actualizado
                Intent intent = new Intent(ACTION_TEXT_UPDATE);
                intent.putExtra(UPDATED_BUTTON_VALUE_KEY, itemNumberStr);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        };
        editText.addTextChangedListener(holder.textWatcher);
    }

    // 4. getItemCount: Devuelve el tamaño del dataset.
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
