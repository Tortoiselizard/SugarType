package com.example.customkeyboardsystemlevel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// El Adapter se encarga de crear ViewHolders y vincular los datos a ellos.
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private ArrayList<Integer> mDataset;

    // Constructor que acepta el conjunto de datos de números.
    public MyAdapter(ArrayList<Integer> myDataset) {
        mDataset = myDataset;
    }

    // 1. ViewHolder: Proporciona una referencia a las vistas para cada elemento de datos.
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        // **Nuevo:** Referencia al EditText
        public EditText editText;

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
        // Obtiene el número de la posición y lo establece en el TextView
        holder.textView.setText(String.valueOf(mDataset.get(position)));

        // Puedes inicializar el EditText aquí si tu dataset tuviera un campo de texto
        // Por ejemplo, holder.editText.setText(mDataset.getText(position));
    }

    // 4. getItemCount: Devuelve el tamaño del dataset.
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
