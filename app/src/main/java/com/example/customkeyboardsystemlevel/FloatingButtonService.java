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
    private View floatingContainerView;
    private LinearLayout buttonContainer;
    private WindowManager.LayoutParams params;
    private DataUpdateReceiver dataUpdateReceiver;

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
        
        dataUpdateReceiver = new DataUpdateReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                dataUpdateReceiver, new IntentFilter(MainActivity.ACTION_DATA_UPDATE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(MainActivity.DATA_LIST_KEY)) {
            ArrayList<Integer> dataList = intent.getIntegerArrayListExtra(MainActivity.DATA_LIST_KEY);
            if (dataList != null) {
                updateButtons(dataList);
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * MODIFICADO: Ahora crea instancias de StatefulButtonView.
     */
    private void updateButtons(ArrayList<Integer> items) {
        buttonContainer.removeAllViews();

        // Puedes establecer un estado por defecto o pasarlo a través del Intent.
        // Aquí, como ejemplo, usamos "working".
        StatefulButtonView.State initialState = StatefulButtonView.State.EDITING;

        for (Integer itemNumber : items) {
            // Crea una nueva instancia de tu CustomView
            StatefulButtonView statefulButton = new StatefulButtonView(this, String.valueOf(itemNumber), initialState);

            // Añade el CustomView al contenedor
            buttonContainer.addView(statefulButton);
        }
    }
    
    private void setupDragListener() {
        floatingContainerView.setOnTouchListener(new View.OnTouchListener() {
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
                        long endTime = System.currentTimeMillis();
                        if (endTime - startTime < MAX_CLICK_DURATION && 
                            Math.abs(event.getRawX() - initialTouchX) < 10 && 
                            Math.abs(event.getRawY() - initialTouchY) < 10) {
                            return false; 
                        }
                        return true;
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
        if (floatingContainerView != null) {
            windowManager.removeView(floatingContainerView);
        }
        if (dataUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataUpdateReceiver);
        }
    }
}
