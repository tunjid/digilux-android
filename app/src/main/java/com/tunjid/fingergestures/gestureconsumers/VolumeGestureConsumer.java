package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.media.AudioManager;

import com.tunjid.fingergestures.App;

public class VolumeGestureConsumer implements GestureConsumer {

    @SuppressLint("StaticFieldLeak")
    private static VolumeGestureConsumer instance;

    static VolumeGestureConsumer getInstance() {
        if (instance == null) instance = new VolumeGestureConsumer();
        return instance;
    }

    private VolumeGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        switch (gesture) {
            case VOLUME_INCREASE:
            case VOLUME_DECREASE:
            case VOLUME_MUTE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        App app = App.getInstance();
        if (app == null) return;

        AudioManager audioManager = app.getSystemService(AudioManager.class);
        if (audioManager == null) return;

        int action = gestureAction == VOLUME_INCREASE ? AudioManager.ADJUST_RAISE
                : gestureAction == VOLUME_DECREASE
                ? AudioManager.ADJUST_LOWER
                : AudioManager.ADJUST_MUTE;


        audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                0);
    }
}

