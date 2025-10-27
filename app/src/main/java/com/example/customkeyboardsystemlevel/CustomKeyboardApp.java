package com.example.customkeyboardsystemlevel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputConnection;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class CustomKeyboardApp extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    // Receptor para los clics (Toque simple)
    private ButtonClickReceiver buttonClickReceiver;
    // NUEVO: Receptor para el deslizamiento (Swipe)
    private ButtonSwipeReceiver buttonSwipeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Inicializar y registrar el receptor de CLIC
        buttonClickReceiver = new ButtonClickReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
            buttonClickReceiver, new IntentFilter(StatefulButtonView.ACTION_BUTTON_CLICKED));
            
        // 2. NUEVO: Inicializar y registrar el receptor de SWIPE
        buttonSwipeReceiver = new ButtonSwipeReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
            buttonSwipeReceiver, new IntentFilter(StatefulButtonView.ACTION_BUTTON_SWIPED));
    }

    @Override
    public View onCreateInputView() {
        View inputView = getLayoutInflater().inflate(R.layout.custom_keyboard_layout, null);
        KeyboardView keyboardView = inputView.findViewById(R.id.keyboard_view);
        Keyboard keyboard = new Keyboard(this, R.xml.custom_keypad);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return inputView;
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (Settings.canDrawOverlays(this)) {
            // <<--- INICIO DE MODIFICACIÓN --->>
            // 1. Lectura de SharedPreferences MOVIDA a MainActivity para centralizar la gestión de datos.
            
            // 2. Iniciar el servicio y pasarle la lista de datos inicial (se mantendrá
            // la lógica de iniciar el servicio solo al mostrar la ventana).
            // La lista real y las actualizaciones se gestionarán a través de un Broadcast.
            
            SharedPreferences prefs = getSharedPreferences(MainActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
            String savedList = prefs.getString(MainActivity.DATA_LIST_KEY, "");
            ArrayList<Integer> dataList = new ArrayList<>();

            if (savedList != null && !savedList.isEmpty()) {
                String[] items = savedList.split(",");
                for (String item : items) {
                    try {
                        dataList.add(Integer.parseInt(item.trim()));
                    } catch (NumberFormatException e) {
                        // Ignorar valores no numéricos si los hubiera
                    }
                }
            }
            
            Intent intent = new Intent(this, FloatingButtonService.class);
            intent.putIntegerArrayListExtra(MainActivity.DATA_LIST_KEY, dataList); // Pasar la lista inicial
            
            // MODIFICACIÓN: Establecer el estado inicial a WORKING
            intent.putExtra(MainActivity.INITIAL_STATE_KEY, MainActivity.STATE_WORKING_VALUE); // WORKING
            
            startService(intent);
            // <<--- FIN DE MODIFICACIÓN --->>
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        stopService(new Intent(this, FloatingButtonService.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Desregistrar el receptor de CLIC
        if (buttonClickReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(buttonClickReceiver);
        }
        // NUEVO: Desregistrar el receptor de SWIPE
        if (buttonSwipeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(buttonSwipeReceiver);
        }
    }

    // El resto de los métodos (onPress, onRelease, etc.) permanecen sin cambios...
    @Override
    public void onPress(int i) {}

    @Override
    public void onRelease(int i) {}

    @Override
    public void onKey(int i, int[] ints) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        inputConnection.commitText(String.valueOf((char) i), 1);
    }

    @Override
    public void onText(CharSequence charSequence) {}

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}
    
    /**
     * BroadcastReceiver para recibir el clic (Toque simple) del botón flotante.
     */
    private class ButtonClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StatefulButtonView.ACTION_BUTTON_CLICKED.equals(intent.getAction())) {
                String buttonValue = intent.getStringExtra(StatefulButtonView.BUTTON_VALUE_KEY);
                
                if (buttonValue != null) {
                    // 1. Obtener el texto asociado de SharedPreferences (del ítem principal)
                    SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                    String textToWrite = prefs.getString(MyAdapter.TEXT_KEY_PREFIX + buttonValue, "");
                    
                    // 2. Escribir el texto en el input seleccionado (InputConnection)
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(textToWrite, 1);
                    }
                }
            }
        }
    }

    /**
     * NUEVO: BroadcastReceiver para recibir el deslizamiento (swipe) del botón flotante.
     */
    private class ButtonSwipeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StatefulButtonView.ACTION_BUTTON_SWIPED.equals(intent.getAction())) {
                String buttonValue = intent.getStringExtra(StatefulButtonView.BUTTON_VALUE_KEY); // e.g., "1"
                String swipeDirection = intent.getStringExtra(StatefulButtonView.SWIPE_DIRECTION_KEY); // e.g., "RIGHT"
                
                if (buttonValue != null && swipeDirection != null && !swipeDirection.equals(StatefulButtonView.Direction.NONE.name())) {
                    
                    // 1. Construir la clave de SharedPreferences para el sub-ítem
                    // e.g., "item_text_" + "1" + "." + "RIGHT" -> "item_text_1.RIGHT"
                    String subItemKey = MyAdapter.TEXT_KEY_PREFIX + buttonValue + "." + swipeDirection;
                    
                    // 2. Obtener el texto asociado de SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                    String textToWrite = prefs.getString(subItemKey, ""); // Usar la clave del sub-ítem
                    
                    // 3. Escribir el texto en el input seleccionado (InputConnection)
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(textToWrite, 1);
                    }
                }
            }
        }
    }
}
