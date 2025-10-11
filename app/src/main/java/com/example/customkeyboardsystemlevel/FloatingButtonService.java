package com.example.customkeyboardsystemlevel;

import android.app.Service;
import android.content.BroadcastReceiver; // Importación añadida
import android.content.Context; // Importación añadida
import android.content.Intent;
import android.content.IntentFilter; // Importación añadida
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Importación añadida

import java.util.ArrayList;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingContainerView;
    private LinearLayout buttonContainer;
    private WindowManager.LayoutParams params;
    private DataUpdateReceiver dataUpdateReceiver; // Referencia al Receiver

    public FloatingButtonService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        floatingContainerView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);
        buttonContainer = floatingContainerView.findViewById(R.id.floating_button_container);

        int layout_parms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout_parms = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_parms = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_parms,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingContainerView, params);

        setupDragListener();
        
        // AÑADIDO: Registrar el BroadcastReceiver
        dataUpdateReceiver = new DataUpdateReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                dataUpdateReceiver, new IntentFilter(MainActivity.ACTION_DATA_UPDATE));
    }

    /**
     * MODIFICACIÓN: Ahora onStartCommand solo procesa el intent de inicio
     * ya que las actualizaciones posteriores vendrán por el BroadcastReceiver.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Al iniciar el servicio, la lista inicial es pasada por CustomKeyboardApp.
        // Opcionalmente, puedes dejar esta lógica para manejar un inicio rápido.
        if (intent != null && intent.hasExtra(MainActivity.DATA_LIST_KEY)) {
            ArrayList<Integer> dataList = intent.getIntegerArrayListExtra(MainActivity.DATA_LIST_KEY);
            if (dataList != null) {
                updateButtons(dataList);
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Nuevo método para crear y mostrar los botones dinámicamente.
     */
    private void updateButtons(ArrayList<Integer> items) {
        // Limpiar botones anteriores para evitar duplicados
        buttonContainer.removeAllViews();

        for (Integer itemNumber : items) {
            Button button = new Button(this);

            // 1. Estilizar el botón mediante código
            int buttonSize = dpToPx(60);
            int buttonMargin = dpToPx(4);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
            buttonParams.setMargins(buttonMargin, buttonMargin, buttonMargin, buttonMargin);
            button.setLayoutParams(buttonParams);

            // 2. Establecer el número correspondiente
            button.setText(String.valueOf(itemNumber));
            button.setTextSize(24);
            button.setTypeface(null, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);

            // Suponiendo que tienes un 'floating_button_background.xml' en res/drawable
            button.setBackgroundResource(R.drawable.floating_button_background);

            // 3. Configurar el listener de clic para cada botón
            button.setOnClickListener(v -> {
                String buttonText = ((Button) v).getText().toString();
                Toast.makeText(FloatingButtonService.this, "Botón: " + buttonText + " presionado", Toast.LENGTH_SHORT).show();
            });

            // 4. Añadir el botón al contenedor
            buttonContainer.addView(button);
        }
    }

    // Método de utilidad para convertir dp a píxeles
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    /**
     * La lógica de arrastre del contenedor.
     */
    private void setupDragListener() {
        floatingContainerView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long startTime;
            private static final int MAX_CLICK_DURATION = 200; // ms para considerar un clic

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingContainerView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                         // Se añade lógica para manejar el clic si no se arrastró
                        long endTime = System.currentTimeMillis();
                        if (endTime - startTime < MAX_CLICK_DURATION && 
                            Math.abs(event.getRawX() - initialTouchX) < 10 && 
                            Math.abs(event.getRawY() - initialTouchY) < 10) {
                            // Si fuera un solo botón, se manejaría un clic aquí.
                            // Como es un contenedor de botones, el clic lo manejarán los botones.
                            return false; 
                        }
                        return true; // Consumir el evento de arrastre
                }
                return false;
            }
        });
    }

    /**
     * AÑADIDO: BroadcastReceiver para recibir actualizaciones de la lista.
     */
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MainActivity.ACTION_DATA_UPDATE.equals(intent.getAction())) {
                ArrayList<Integer> dataList = intent.getIntegerArrayListExtra(MainActivity.DATA_LIST_KEY);
                if (dataList != null) {
                    updateButtons(dataList); // Actualiza los botones con la nueva lista
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingContainerView != null) {
            windowManager.removeView(floatingContainerView);
        }
        // AÑADIDO: Desregistrar el BroadcastReceiver
        if (dataUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataUpdateReceiver);
        }
    }
}
