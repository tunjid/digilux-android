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
    public boolean accepts(@GestureAction int gesture) {
        return gesture == DO_NOTHING;
    }

    @Override
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        // Do nothing
    }
}

