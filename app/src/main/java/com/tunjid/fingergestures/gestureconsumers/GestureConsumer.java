package com.tunjid.fingergestures.gestureconsumers;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface GestureConsumer {

    int ZERO_PERCENT = 0;
    int FIFTY_PERCENT = 50;
    int HUNDRED_PERCENT = 100;

    int INCREASE_BRIGHTNESS = 0;
    int REDUCE_BRIGHTNESS = 1;
    int MAXIMIZE_BRIGHTNESS = 2;
    int MINIMIZE_BRIGHTNESS = 3;
    // int NIGHT_MODE_ON = 4; DO NOT REMOVE FOR LEGACY REASONS
    // int NIGHT_MODE_OFF = 5; DO NOT REMOVE FOR LEGACY REASONS
    int NOTIFICATION_UP = 6;
    int NOTIFICATION_DOWN = 7;
    int DO_NOTHING = 8;
    int TOGGLE_FLASHLIGHT = 9;
    int TOGGLE_DOCK = 10;
    int TOGGLE_AUTO_ROTATE = 11;
    int NOTIFICATION_TOGGLE = 12;
    int GLOBAL_HOME = 13;
    int GLOBAL_BACK = 14;
    int GLOBAL_RECENTS = 15;
    int GLOBAL_SPLIT_SCREEN = 16;
    int GLOBAL_POWER_DIALOG = 17;
    int SHOW_POPUP = 18;
    int INCREASE_AUDIO = 19;
    int REDUCE_AUDIO = 20;
    int GLOBAL_LOCK_SCREEN = 21;
    int GLOBAL_TAKE_SCREENSHOT = 22;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INCREASE_BRIGHTNESS, REDUCE_BRIGHTNESS, MAXIMIZE_BRIGHTNESS, MINIMIZE_BRIGHTNESS,
            NOTIFICATION_DOWN, NOTIFICATION_UP, NOTIFICATION_TOGGLE, DO_NOTHING,
            TOGGLE_FLASHLIGHT, TOGGLE_DOCK, TOGGLE_AUTO_ROTATE,
            GLOBAL_HOME, GLOBAL_BACK, GLOBAL_RECENTS, GLOBAL_SPLIT_SCREEN, GLOBAL_POWER_DIALOG,
            SHOW_POPUP, REDUCE_AUDIO, INCREASE_AUDIO, GLOBAL_LOCK_SCREEN, GLOBAL_TAKE_SCREENSHOT})
    @interface GestureAction {}

    void onGestureActionTriggered(@GestureAction int gestureAction);

    boolean accepts(@GestureAction int gesture);

    static float normalizePercentageToFraction(int percentage) {
        return percentage / 100F;
    }

}
