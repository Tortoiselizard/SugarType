package com.example.customkeyboardsystemlevel;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    
    // MODIFICADO: Lista para rastrear todas las vistas flotantes individuales (una por botón).
    private final ArrayList<View> floatingViews = new ArrayList<>();
    
    private DataUpdateReceiver dataUpdateReceiver;
    
    // NUEVA VARIABLE: Para almacenar el estado actual (WORKING o EDITING)
    private StatefulButtonView.State currentButtonState = StatefulButtonView.State.WORKING; 

    public FloatingButtonService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Se elimina la inflación del layout principal y la adición inicial de una sola vista.
        // Ahora, cada CustomView será su propia ventana flotante.
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Ya no se llama a setupDragListener() aquí. Se llama dentro de updateButtons()
        // para cada botón individual.
        
        dataUpdateReceiver = new DataUpdateReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                dataUpdateReceiver, new IntentFilter(MainActivity.ACTION_DATA_UPDATE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Leer el estado del Intent y actualizar la variable de clase
            if (intent.hasExtra(MainActivity.INITIAL_STATE_KEY)) {
                int stateIndex = intent.getIntExtra(MainActivity.INITIAL_STATE_KEY, MainActivity.STATE_WORKING_VALUE); 
                // Convertir el entero (0 o 1) a la constante enum
                this.currentButtonState = StatefulButtonView.State.values()[stateIndex];
            }
            
            if (intent.hasExtra(MainActivity.DATA_LIST_KEY)) {
                ArrayList<Integer> dataList = intent.getIntegerArrayListExtra(MainActivity.DATA_LIST_KEY);
                if (dataList != null) {
                    updateButtons(dataList);
                }
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * MODIFICADO: Ahora crea instancias de StatefulButtonView y añade cada una 
     * como una ventana flotante separada al WindowManager.
     */
    private void updateButtons(ArrayList<Integer> items) {
        // PASO 1: Remover todas las vistas flotantes actuales.
        for (View view : floatingViews) {
            windowManager.removeView(view);
        }
        floatingViews.clear(); 

        StatefulButtonView.State initialState = this.currentButtonState; 
        int verticalOffset = 0; // Para espaciar los botones verticalmente
        final int OFFSET_INCREMENT = 150; // Espaciado vertical entre botones

        int layout_parms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout_parms = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_parms = WindowManager.LayoutParams.TYPE_PHONE;
        }

        for (Integer itemNumber : items) {
            // PASO 2: Crear la CustomView (será la ventana flotante).
            final StatefulButtonView statefulButton = new StatefulButtonView(
                this, 
                String.valueOf(itemNumber), 
                initialState
            );

            // PASO 3: Crear un WindowManager.LayoutParams para esta VISTA INDIVIDUAL.
            final WindowManager.LayoutParams individualParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_parms,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

            individualParams.gravity = Gravity.TOP | Gravity.START;
            individualParams.x = 0;
            // POSICIÓN ÚNICA: Usar el offset vertical para que no se superpongan.
            individualParams.y = 100 + verticalOffset; 
            verticalOffset += OFFSET_INCREMENT;
            
            // PASO 4: Añadir el listener de arrastre a esta nueva vista y sus parámetros.
            setupDragListener(statefulButton, individualParams);

            // PASO 5: Añadir la vista al WindowManager. Esto crea la ventana flotante.
            windowManager.addView(statefulButton, individualParams);
            
            // PASO 6: Rastrear la vista para poder eliminarla más tarde.
            floatingViews.add(statefulButton);
        }
    }
    
    /**
     * MODIFICADO: Ahora recibe la vista específica y sus parámetros para mover solo esa vista.
     */
    private void setupDragListener(final View viewToDrag, final WindowManager.LayoutParams viewParams) {
        viewToDrag.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long startTime;
            private static final int MAX_CLICK_DURATION = 200;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Usamos los parámetros de la VISTA ESPECÍFICA
                        initialX = viewParams.x;
                        initialY = viewParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // Actualizamos los parámetros de la VISTA ESPECÍFICA
                        viewParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        viewParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        // Usamos updateViewLayout para actualizar SOLO esa vista.
                        windowManager.updateViewLayout(viewToDrag, viewParams); 
                        return true;
                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        // Lógica de "click" vs "drag"
                        if (endTime - startTime < MAX_CLICK_DURATION && 
                            Math.abs(event.getRawX() - initialTouchX) < 10 && 
                            Math.abs(event.getRawY() - initialTouchY) < 10) {
                            return false; // Permite el evento de click.
                        }
                        return true; // Consumir el evento (fue un arrastre).
                }
                return false;
            }
        });
    }

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MainActivity.ACTION_DATA_UPDATE.equals(intent.getAction())) {
                ArrayList<Integer> dataList = intent.getIntegerArrayListExtra(MainActivity.DATA_LIST_KEY);
                if (dataList != null) {
                    updateButtons(dataList); 
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Recorrer la lista y eliminar cada vista flotante individualmente
        for (View view : floatingViews) {
            if (view != null && view.getParent() != null) {
                windowManager.removeView(view);
            }
        }
        floatingViews.clear(); 
        
        if (dataUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataUpdateReceiver);
        }
    }
}
