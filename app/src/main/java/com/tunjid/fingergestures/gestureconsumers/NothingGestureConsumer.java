package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;

import java.util.HashSet;
import java.util.Set;

public class NothingGestureConsumer implements GestureConsumer {

    @SuppressLint("StaticFieldLeak")
    private static NothingGestureConsumer instance;

    private final Set<Integer> gestures;

    static NothingGestureConsumer getInstance() {
        if (instance == null) instance = new NothingGestureConsumer();
        return instance;
    }

    private NothingGestureConsumer() {
        gestures = new HashSet<>();
        gestures.add(DO_NOTHING);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case DO_NOTHING:
                break;
        }
    }

    @Override
    public Set<Integer> gestures() {
        return gestures;
    }
}

