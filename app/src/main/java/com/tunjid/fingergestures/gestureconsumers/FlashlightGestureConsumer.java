package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.tunjid.fingergestures.App;

import java.util.HashSet;
import java.util.Set;

public class FlashlightGestureConsumer implements GestureConsumer {

    private final Set<Integer> gestures;
    private final App app;
    private boolean isTorchOn;

    private static FlashlightGestureConsumer instance;

    static FlashlightGestureConsumer getInstance() {
        if (instance == null) instance = new FlashlightGestureConsumer();
        return instance;
    }

    private FlashlightGestureConsumer() {
        app = App.getInstance();
        gestures = new HashSet<>();
        gestures.add(TOGGLE_FLASHLIGHT);

        CameraManager cameraManager = app.getSystemService(CameraManager.class);
        if (cameraManager == null) return;

        cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                isTorchOn = enabled;
            }
        }, new Handler());
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case TOGGLE_FLASHLIGHT:
                CameraManager cameraManager = app.getSystemService(CameraManager.class);
                if (cameraManager == null) return;

                try {cameraManager.setTorchMode(cameraManager.getCameraIdList()[0], !isTorchOn);}
                catch (Exception e) {e.printStackTrace();}
                break;
        }
    }

    @Override
    public Set<Integer> gestures() {
        return gestures;
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

