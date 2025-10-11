package com.example.customkeyboardsystemlevel;

import android.content.Intent;
import android.content.SharedPreferences; // Importación añadida
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList; // Importación añadida

public class CustomKeyboardApp extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

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
            // 1. Leer los datos desde SharedPreferences
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

            // 2. Iniciar el servicio y pasarle la lista de datos
            Intent intent = new Intent(this, FloatingButtonService.class);
            intent.putIntegerArrayListExtra("dataList", dataList);
            startService(intent);
            // <<--- FIN DE MODIFICACIÓN --->>
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        stopService(new Intent(this, FloatingButtonService.class));
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
}
