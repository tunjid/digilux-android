package com.tunjid.fingergestures.gestureconsumers;


import android.content.SharedPreferences;
import android.support.annotation.IntDef;

import com.tunjid.fingergestures.Application;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.content.Context.MODE_PRIVATE;

public class GestureUtils {

    private static final String BRIGHTNESS_PREFS = "brightness prefs";

    static final int INCREASE_BRIGHTNESS = 0;
    static final int REDUCE_BRIGHTNESS = 1;
    static final int MAXIMIZE_BRIGHTNESS = 2;
    static final int MININIMIZE_BRIGHTNESS = 3;
    static final int NIGHT_MODE_ON = 4;
    static final int NIGHT_MODE_OFF = 5;
    static final int NOTIFICATION_UP = 6;
    static final int NOTIFICATION_DOWN = 7;
    static final int DO_NOTHING = 8;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INCREASE_BRIGHTNESS, REDUCE_BRIGHTNESS, MAXIMIZE_BRIGHTNESS, MININIMIZE_BRIGHTNESS,
            NIGHT_MODE_ON, NIGHT_MODE_OFF, NOTIFICATION_DOWN, NOTIFICATION_UP, DO_NOTHING})
    @interface GestureAction {}

    public static int normalizePercetageToByte(int percentage) {
        return (int) (BrightnessGestureConsumer.MAX_BRIGHTNESS * (percentage / 100F));
    }

    public static SharedPreferences getPreferences() {
        return Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
    }
}
