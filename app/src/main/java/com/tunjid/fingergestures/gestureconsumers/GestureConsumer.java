package com.tunjid.fingergestures.gestureconsumers;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface GestureConsumer {

    int INCREASE_BRIGHTNESS = 0;
    int REDUCE_BRIGHTNESS = 1;
    int MAXIMIZE_BRIGHTNESS = 2;
    int MINIMIZE_BRIGHTNESS = 3;
    int NIGHT_MODE_ON = 4;
    int NIGHT_MODE_OFF = 5;
    int NOTIFICATION_UP = 6;
    int NOTIFICATION_DOWN = 7;
    int DO_NOTHING = 8;
    int TOGGLE_FLASHLIGHT = 9;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INCREASE_BRIGHTNESS, REDUCE_BRIGHTNESS, MAXIMIZE_BRIGHTNESS, MINIMIZE_BRIGHTNESS,
            NIGHT_MODE_ON, NIGHT_MODE_OFF, NOTIFICATION_DOWN, NOTIFICATION_UP, DO_NOTHING,
            TOGGLE_FLASHLIGHT})
    @interface GestureAction {}

    void onGestureActionTriggered(@GestureAction int gestureAction);

    boolean accepts(@GestureAction int gesture);
}
