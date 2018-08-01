package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.tunjid.fingergestures.App;

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN;

public class GlobalActionGestureConsumer implements GestureConsumer {

    public static final String ACTION_GLOBAL_ACTION = "GlobalActionConsumer action";
    public static final String EXTRA_GLOBAL_ACTION = "GlobalActionConsumer action extra";

    @SuppressLint("StaticFieldLeak")
    private static GlobalActionGestureConsumer instance;

    static GlobalActionGestureConsumer getInstance() {
        if (instance == null) instance = new GlobalActionGestureConsumer();
        return instance;
    }

    private GlobalActionGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        switch (gesture) {
            case GLOBAL_HOME:
            case GLOBAL_BACK:
            case GLOBAL_RECENTS:
            case GLOBAL_SPLIT_SCREEN:
            case GLOBAL_POWER_DIALOG:
                return true;
            default:
                return false;
        }
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        App app = App.getInstance();
        if (app == null) return;

        int globalAction = -1;

        switch (gestureAction) {
            case GLOBAL_HOME:
                globalAction = GLOBAL_ACTION_HOME;
                break;
            case GLOBAL_BACK:
                globalAction = GLOBAL_ACTION_BACK;
                break;
            case GLOBAL_RECENTS:
                globalAction = GLOBAL_ACTION_RECENTS;
                break;
            case GLOBAL_SPLIT_SCREEN:
                globalAction = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN;
                break;
            case GLOBAL_POWER_DIALOG:
                globalAction = GLOBAL_ACTION_POWER_DIALOG;
                break;
        }

        if (globalAction == -1) return;

        Intent intent = new Intent(ACTION_GLOBAL_ACTION);
        intent.putExtra(EXTRA_GLOBAL_ACTION, globalAction);

        LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
    }
}

