package com.example.customkeyboardsystemlevel;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class StatefulButtonView extends FrameLayout {

    public enum State {
        WORKING,
        EDITING
    }

    private State currentState;
    private String buttonValue;
    private Button button;

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

        button.setOnClickListener(v -> {
            switch (currentState) {
                case WORKING:
                    // Acción para el estado "working": No se realiza ninguna acción de movimiento.
                    Toast.makeText(getContext(), "Botón (Working): " + buttonValue, Toast.LENGTH_SHORT).show();
                    break;
                case EDITING:
                    // Acción para el estado "editing"
                    Toast.makeText(getContext(), "Botón (Editing): " + buttonValue + " - MOVER", Toast.LENGTH_SHORT).show();
                    
                    // Lógica para mover la vista 10dp hacia abajo
                    // Se aplica la nueva traslación Y al FrameLayout (StatefulButtonView)
                    this.setTranslationY(this.getTranslationY() + moveDownPx); 
                    
                    break;
            }
        });
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
