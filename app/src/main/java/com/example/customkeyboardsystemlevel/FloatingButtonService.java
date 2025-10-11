package com.example.customkeyboardsystemlevel;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface; // Importación añadida
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout; // Importación añadida
import android.widget.Toast;

import java.util.ArrayList; // Importación añadida

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingContainerView; // Renombrado de floatingButtonView
    private LinearLayout buttonContainer; // Referencia al contenedor de botones
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

        // Inflar el layout contenedor
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

        // Configurar el listener para arrastrar el contenedor
        setupDragListener();
    }

    /**
     * MODIFICACIÓN: Se ejecuta cada vez que se llama a startService.
     * Aquí recibimos la lista de datos y actualizamos los botones.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("dataList")) {
            ArrayList<Integer> dataList = intent.getIntegerArrayListExtra("dataList");
            if (dataList != null) {
                updateButtons(dataList);
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * MODIFICACIÓN: Nuevo método para crear y mostrar los botones dinámicamente.
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
     * MODIFICACIÓN: La lógica de arrastre ahora está en su propio método.
     * El listener se asigna al contenedor principal.
     */
    private void setupDragListener() {
        floatingContainerView.setOnTouchListener(new View.OnTouchListener() {
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
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingContainerView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                         // Se consume el evento para que el gesto de arrastre no se confunda con un clic
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingContainerView != null) {
            windowManager.removeView(floatingContainerView);
        }
    }
}
