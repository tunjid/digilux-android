package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;

public class NothingGestureConsumer implements GestureConsumer {

    @SuppressLint("StaticFieldLeak")
    private static NothingGestureConsumer instance;

    static NothingGestureConsumer getInstance() {
        if (instance == null) instance = new NothingGestureConsumer();
        return instance;
    }

    private NothingGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        switch (gesture) {
            case DO_NOTHING:
                return true;
            default:
                return false;
        }
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case DO_NOTHING:
                break;
        }
    }
}

