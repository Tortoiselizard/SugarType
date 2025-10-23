package com.example.customkeyboardsystemlevel;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
    
    // Lista para rastrear todas las vistas flotantes individuales (una por botón).
    private final ArrayList<View> floatingViews = new ArrayList<>();
    
    private DataUpdateReceiver dataUpdateReceiver;
    
    // Para almacenar el estado actual (WORKING o EDITING)
    private StatefulButtonView.State currentButtonState = StatefulButtonView.State.WORKING; 
    
    // CONSTANTE: Nombre para SharedPreferences
    private static final String PREFS_NAME = "FloatingButtonPrefs";
    private static final String X_POS_KEY_PREFIX = "button_x_";
    private static final String Y_POS_KEY_PREFIX = "button_y_";
    // Posición inicial por defecto si no se encuentra ninguna guardada.
    private static final int DEFAULT_INITIAL_X = 0;
    private static final int DEFAULT_INITIAL_Y_OFFSET = 100;

    public FloatingButtonService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

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
     * MODIFICADO: Ahora crea instancias de StatefulButtonView, carga su posición 
     * guardada, *carga el texto asociado* y añade cada una como una ventana flotante 
     * separada al WindowManager.
     */
    private void updateButtons(ArrayList<Integer> items) {
        // PASO 1: Remover todas las vistas flotantes actuales.
        for (View view : floatingViews) {
            windowManager.removeView(view);
        }
        floatingViews.clear(); 

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
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
            
            // ************ NUEVA MODIFICACIÓN CLAVE (CARGA DE TEXTO) ************
            // Cargar y establecer el texto del botón desde SharedPreferences (el contenido del EditText)
            statefulButton.setButtonTextFromPrefs();
            // ************ FIN NUEVA MODIFICACIÓN CLAVE (CARGA DE TEXTO) ************

            // Esto asegura que el FrameLayout (padre) reciba los eventos táctiles
            // y no solo el Button (hijo).
            statefulButton.findViewById(R.id.stateful_button).setClickable(false);

            // PASO 3: Crear un WindowManager.LayoutParams para esta VISTA INDIVIDUAL.
            final WindowManager.LayoutParams individualParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_parms,
                // MODIFICACIÓN: Si está en modo WORKING, añadimos FLAG_NOT_TOUCH_MODAL para 
                // asegurar que los eventos de click pasen a la vista, pero no afecten el arrastre.
                // En modo EDITING, se mantiene solo FLAG_NOT_FOCUSABLE.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                (initialState == StatefulButtonView.State.WORKING ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL : 0),
                PixelFormat.TRANSLUCENT
            );

            individualParams.gravity = Gravity.TOP | Gravity.START;
            
            // ************ MODIFICACIÓN CLAVE (CARGA DE POSICIÓN) ************
            // Cargar posición guardada o usar posición por defecto si no existe.
            int savedX = prefs.getInt(X_POS_KEY_PREFIX + itemNumber, DEFAULT_INITIAL_X);
            // Usar una posición Y inicial basada en el índice SÓLO si no hay una posición Y guardada.
            int defaultY = DEFAULT_INITIAL_Y_OFFSET + verticalOffset;
            int savedY = prefs.getInt(Y_POS_KEY_PREFIX + itemNumber, defaultY);

            individualParams.x = savedX;
            individualParams.y = savedY;
            // Solo incrementamos el offset para la próxima vista si estamos usando la posición Y por defecto
            // Es una heurística simple: si ya tiene posición guardada, asumimos que no necesitamos el espaciado
            // automático, pero si no la tiene, le damos un espaciado inicial.
            if (savedY == defaultY) {
                 verticalOffset += OFFSET_INCREMENT;
            }
            // ************ FIN MODIFICACIÓN CLAVE (CARGA DE POSICIÓN) ************

            
            // PASO 4: Añadir el listener de arrastre a esta nueva vista y sus parámetros.
            // MODIFICACIÓN: Se añade el itemNumber para guardar la posición.
            setupDragListener(statefulButton, individualParams, initialState, itemNumber);

            // PASO 5: Añadir la vista al WindowManager. Esto crea la ventana flotante.
            windowManager.addView(statefulButton, individualParams);
            
            // PASO 6: Rastrear la vista para poder eliminarla más tarde.
            floatingViews.add(statefulButton);
        }
    }
    
    /**
     * MODIFICADO: Se añade el parámetro itemNumber para guardar la posición al finalizar el arrastre.
     */
    private void setupDragListener(final View viewToDrag, final WindowManager.LayoutParams viewParams, final StatefulButtonView.State state, final int itemNumber) {
        viewToDrag.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long startTime;
            private boolean isMoving = false; // Bandera para rastrear si realmente hubo arrastre.
            private static final int MAX_CLICK_DURATION = 200;
            // Tolerancia de movimiento en píxeles para distinguir click de drag
            private static final int MAX_MOVE_TOLERANCE = 10; 

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                
                // Si no está en modo EDITING, el listener de arrastre 
                // debería ignorar el evento MOVE para impedir el arrastre.
                if (state != StatefulButtonView.State.EDITING && event.getAction() == MotionEvent.ACTION_MOVE) {
                    return false; 
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Usamos los parámetros de la VISTA ESPECÍFICA
                        initialX = viewParams.x;
                        initialY = viewParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        isMoving = false; // Reiniciar la bandera
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // Solo permitimos el arrastre si el estado es EDITING.
                        if (state == StatefulButtonView.State.EDITING) {
                            // Solo procesar el movimiento si el movimiento excede la tolerancia para evitar
                            // un arrastre accidental al intentar un click.
                            if (Math.abs(event.getRawX() - initialTouchX) > MAX_MOVE_TOLERANCE || 
                                Math.abs(event.getRawY() - initialTouchY) > MAX_MOVE_TOLERANCE) {
                                
                                isMoving = true; // El arrastre ha comenzado.
                                // Actualizamos los parámetros de la VISTA ESPECÍFICA
                                viewParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                                viewParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                                // Usamos updateViewLayout para actualizar SOLO esa vista.
                                windowManager.updateViewLayout(viewToDrag, viewParams); 
                            }
                            return true;
                        }
                        return false; 
                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        
                        // Lógica de "click" vs "drag"
                        if (endTime - startTime < MAX_CLICK_DURATION && 
                            !isMoving &&
                            Math.abs(event.getRawX() - initialTouchX) < MAX_MOVE_TOLERANCE && 
                            Math.abs(event.getRawY() - initialTouchY) < MAX_MOVE_TOLERANCE) {
                            
                            // Si es un click, llamamos a la acción de la CustomView.
                            if (viewToDrag instanceof StatefulButtonView) {
                                ((StatefulButtonView) viewToDrag).performClickAction();
                            }
                            
                            return true; // Consumir el evento de clic.
                        } else if (state == StatefulButtonView.State.EDITING && isMoving) {
                            // ************ MODIFICACIÓN CLAVE (GUARDADO DE POSICIÓN) ************
                            // Si fue un arrastre en modo EDITING, guardar la posición final.
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putInt(X_POS_KEY_PREFIX + itemNumber, viewParams.x);
                            editor.putInt(Y_POS_KEY_PREFIX + itemNumber, viewParams.y);
                            editor.apply();
                            // ************ FIN MODIFICACIÓN CLAVE (GUARDADO DE POSICIÓN) ************
                        }
                        
                        return true; // Consumir el evento (fue un arrastre o un click).
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
                
                // MODIFICACIÓN: Leer el estado del Intent antes de llamar a updateButtons.
                if (intent.hasExtra(MainActivity.INITIAL_STATE_KEY)) {
                    int stateIndex = intent.getIntExtra(MainActivity.INITIAL_STATE_KEY, MainActivity.STATE_WORKING_VALUE); 
                    FloatingButtonService.this.currentButtonState = StatefulButtonView.State.values()[stateIndex];
                }
                
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
