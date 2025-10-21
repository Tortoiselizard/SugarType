package com.example.customkeyboardsystemlevel;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class StatefulButtonView extends FrameLayout {

    public enum State {
        WORKING,
        EDITING
    }

    private State currentState;
    private String buttonValue;
    private Button button;
    
    // NUEVA CONSTANTE para la acción de Broadcast cuando se hace clic
    public static final String ACTION_BUTTON_CLICKED = "com.example.customkeyboardsystemlevel.BUTTON_CLICKED";
    public static final String BUTTON_VALUE_KEY = "buttonValue";

    // Constante para el desplazamiento en DP
    private static final int MOVE_DOWN_DP = 10;
    
    // Variable para almacenar el desplazamiento en píxeles (calculado una vez)
    private float moveDownPx;

    public StatefulButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Calcula la conversión aquí para asegurarte de que moveDownPx esté inicializado
        moveDownPx = dpToPx(context, MOVE_DOWN_DP);
        init(context, attrs);
    }

    public StatefulButtonView(Context context, String buttonValue, State initialState) {
        super(context);
        this.buttonValue = buttonValue;
        this.currentState = initialState;
        // Calcula la conversión aquí para asegurarte de que moveDownPx esté inicializado
        moveDownPx = dpToPx(context, MOVE_DOWN_DP);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.stateful_button_layout, this, true);
        button = findViewById(R.id.stateful_button);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.StatefulButtonView,
                    0, 0);

            try {
                int stateIndex = a.getInt(R.styleable.StatefulButtonView_state, 0);
                currentState = State.values()[stateIndex];
                buttonValue = a.getString(R.styleable.StatefulButtonView_buttonValue);
            } finally {
                a.recycle();
            }
        }

        updateButtonState();
        button.setText(buttonValue);

        // MODIFICADO: Se elimina el OnClickListener. 
        // La acción de clic ahora se gestionará desde el FloatingButtonService.
        // Se puede dejar el botón 'clickable' para accesibilidad, pero se debe asegurar que
        // no consuma el evento 'ACTION_DOWN' si es posible, o que el Listener esté en el FrameLayout padre.
    }
    
    /**
     * MODIFICADO: Ejecuta la acción de clic y ahora envía un Broadcast local con el valor
     * del botón cuando está en modo WORKING.
     */
    public void performClickAction() {
        switch (currentState) {
            case WORKING:
                // Acción para el estado "working"
                Toast.makeText(getContext(), "Botón (Working): " + buttonValue + " - ESCRIBIENDO", Toast.LENGTH_SHORT).show();
                
                // *** MODIFICACIÓN CLAVE: Enviar Broadcast local al hacer clic ***
                Intent intent = new Intent(ACTION_BUTTON_CLICKED);
                intent.putExtra(BUTTON_VALUE_KEY, buttonValue); // El valor del botón (el número de ítem)
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                
                break;
            case EDITING:
                // Acción para el estado "editing"
                Toast.makeText(getContext(), "Botón (Editing): " + buttonValue + " - MOVER", Toast.LENGTH_SHORT).show();
                
                break;
        }
    }

    public void setState(State state) {
        this.currentState = state;
        updateButtonState();
    }

    public State getState() {
        return currentState;
    }

    public void setButtonValue(String value) {
        this.buttonValue = value;
        button.setText(value);
    }
    
    public String getButtonValue() {
        return this.buttonValue;
    }

    private void updateButtonState() {
        // Establecer la opacidad al 100% (1.0f) para ambos estados.
        button.setAlpha(1.0f); 

        if (currentState == State.EDITING) {
            // Asignar el nuevo fondo con borde.
            // Asegúrate de que este Drawable exista en res/drawable/
            button.setBackgroundResource(R.drawable.floating_button_background_editing);
        } else {
            // Volver al fondo original para el estado normal "working".
            button.setBackgroundResource(R.drawable.floating_button_background);
        }
    }
    
    /**
     * Convierte DP a Píxeles usando TypedValue.
     * Este es el método recomendado por Android para la conversión de unidades.
     * * @param context El Context.
     * @param dp La dimensión en Density-independent Pixels (DP).
     * @return La dimensión convertida en píxeles.
     */
    private float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                (float) dp, 
                context.getResources().getDisplayMetrics()
        );
    }
}
