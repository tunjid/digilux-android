package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tunjid.fingergestures.App;

public class FlashlightGestureConsumer implements GestureConsumer {

    private boolean isCallbackRegistered;
    private boolean isTorchOn;

    private static FlashlightGestureConsumer instance;

    static FlashlightGestureConsumer getInstance() {
        if (instance == null) instance = new FlashlightGestureConsumer();
        return instance;
    }

    private FlashlightGestureConsumer() {
        isCallbackRegistered = registerTorchCallback(App.getInstance());
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        return gesture == TOGGLE_FLASHLIGHT;
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case TOGGLE_FLASHLIGHT:
                App app = App.getInstance();
                if (!isCallbackRegistered) isCallbackRegistered = registerTorchCallback(app);
                if (!isCallbackRegistered) return;

                CameraManager cameraManager = app.getSystemService(CameraManager.class);
                if (cameraManager == null) return;

                try {cameraManager.setTorchMode(cameraManager.getCameraIdList()[0], !isTorchOn);}
                catch (Exception e) {e.printStackTrace();}
                break;
        }
    }

    private boolean registerTorchCallback(@Nullable App app) {
        if (app == null) return false;

        CameraManager cameraManager = app.getSystemService(CameraManager.class);
        if (cameraManager == null) return false;

        cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                isTorchOn = enabled;
            }
        }, new Handler());

        return true;
    }

//     try {
//        for (String id : cameraManager.getCameraIdList()) {
//            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
//            Boolean flag = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
//            // Turn on the flash if camera has one
//            if (flag != null && flag) {
//                cameraManager.setTorchMode(id, gestureAction == FLASHLIGHT_ON);
//            }
//        }
//
//    }
}

