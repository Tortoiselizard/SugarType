package com.example.customkeyboardsystemlevel;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class CustomKeyboardApp extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    @Override
    public View onCreateInputView() {
        // Inflamos el LinearLayout contenedor (la vista raíz)
        View inputView = getLayoutInflater().inflate(R.layout.custom_keyboard_layout, null);
        
        // Buscamos la KeyboardView dentro del contenedor usando su ID
        KeyboardView keyboardView = inputView.findViewById(R.id.keyboard_view);
        
        // El resto de la lógica sigue igual
        Keyboard keyboard = new Keyboard(this, R.xml.custom_keypad);
        keyboardView.setKeyboard(keyboard);

        keyboardView.setOnKeyboardActionListener(this);
        
        // Devolvemos la vista raíz (el LinearLayout con el spacer de 1dp)
        return inputView; 
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingButtonService.class));
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
            stopService(new Intent(this, FloatingButtonService.class));
    }
    

    @Override
    public void onPress(int i) {
    }

    @Override
    public void onRelease(int i) {
    }

    @Override
    public void onKey(int i, int[] ints) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null){
            return;
        }
        inputConnection.commitText(String.valueOf((char) i),1);
    }

    @Override
    public void onText(CharSequence charSequence) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}
