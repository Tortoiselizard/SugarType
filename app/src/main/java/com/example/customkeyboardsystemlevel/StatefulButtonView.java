package com.example.customkeyboardsystemlevel;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
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

    public StatefulButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatefulButtonView(Context context, String buttonValue, State initialState) {
        super(context);
        this.buttonValue = buttonValue;
        this.currentState = initialState;
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
                    // Acción para el estado "working"
                    Toast.makeText(getContext(), "Botón (Working): " + buttonValue, Toast.LENGTH_SHORT).show();
                    break;
                case EDITING:
                    // Acción para el estado "editing"
                    Toast.makeText(getContext(), "Botón (Editing): " + buttonValue, Toast.LENGTH_SHORT).show();
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
}
