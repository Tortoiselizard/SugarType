package com.example.customkeyboardsystemlevel;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton; // Importación necesaria
import android.widget.Toast;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingButtonView;
    private WindowManager.LayoutParams params;

    public FloatingButtonService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        floatingButtonView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);

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
        windowManager.addView(floatingButtonView, params);

        // 1. Configurar un Click Listener para cada botón
        setupButtonListeners();

        // 2. Configurar el Touch Listener en el contenedor para arrastrar el conjunto de botones (Grid)
        floatingButtonView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        // Devolvemos false para permitir que el evento se propague a los botones individuales
                        // para que puedan manejar el ACTION_UP como un click.
                        return false; 
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingButtonView, params);
                        // Devolvemos true para indicar que hemos manejado el movimiento (arrastre)
                        return true; 
                    case MotionEvent.ACTION_UP:
                         // Devolvemos false para que el evento de "click" (ACTION_DOWN seguido de ACTION_UP) 
                         // sea manejado por el click listener de cada ImageButton.
                        return false; 
                }
                return false;
            }
        });
    }

    private void setupButtonListeners() {
        int[] buttonIds = {
            R.id.floating_button_1, R.id.floating_button_2, R.id.floating_button_3,
            R.id.floating_button_4, R.id.floating_button_5, R.id.floating_button_6,
            R.id.floating_button_7, R.id.floating_button_8, R.id.floating_button_9
        };

        for (int id : buttonIds) {
            ImageButton button = floatingButtonView.findViewById(id);
            if (button != null) {
                // Configuramos un simple click listener para la acción del botón
                button.setOnClickListener(v -> {
                    // Usamos el ID para identificar qué botón fue presionado
                    String buttonName = getResources().getResourceEntryName(v.getId());
                    Toast.makeText(FloatingButtonService.this, "Botón: " + buttonName + " presionado", Toast.LENGTH_SHORT).show();
                    
                    // Lógica adicional para cada botón iría aquí...
                });
                
                // Es crucial anular cualquier otro TouchListener que pueda interferir con el arrastre
                // del layout padre, pero en este caso, la configuración de los onTouch y onClick
                // en el padre e hijo respectivamente debería manejarlo.
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButtonView != null) {
            windowManager.removeView(floatingButtonView);
        }
    }
}
