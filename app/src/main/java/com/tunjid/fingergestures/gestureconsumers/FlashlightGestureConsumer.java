package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraManager;

import com.tunjid.fingergestures.App;

import java.util.HashSet;
import java.util.Set;

public class FlashlightGestureConsumer implements GestureConsumer {

    private final Set<Integer> gestures;
    private final App app;

    private static FlashlightGestureConsumer instance;

    static FlashlightGestureConsumer getInstance() {
        if (instance == null) instance = new FlashlightGestureConsumer();
        return instance;
    }

    private FlashlightGestureConsumer() {
        app = App.getInstance();
        gestures = new HashSet<>();
        gestures.add(FLASHLIGHT_ON);
        gestures.add(FLASHLIGHT_OFF);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case FLASHLIGHT_ON:
            case FLASHLIGHT_OFF:
                CameraManager cameraManager = app.getSystemService(CameraManager.class);
                if (cameraManager == null) return;

                boolean state = gestureAction == FLASHLIGHT_ON;

                try {cameraManager.setTorchMode(cameraManager.getCameraIdList()[0], state);}
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

