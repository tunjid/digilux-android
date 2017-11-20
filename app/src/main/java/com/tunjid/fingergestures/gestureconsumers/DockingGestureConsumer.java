package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.activities.DockingActivity;

public class DockingGestureConsumer implements GestureConsumer {

    public static final String ACTION_TOGGLE_DOCK = "DockingGestureConsumer toggle dock";

    private static DockingGestureConsumer instance;

    static DockingGestureConsumer getInstance() {
        if (instance == null) instance = new DockingGestureConsumer();
        return instance;
    }

    private DockingGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        return gesture == TOGGLE_DOCK;
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case TOGGLE_DOCK:
                App app = App.getInstance();
                if (app == null) return;

                Intent intent = new Intent(app, DockingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);

                app.startActivity(intent);
                break;
        }
    }
}

