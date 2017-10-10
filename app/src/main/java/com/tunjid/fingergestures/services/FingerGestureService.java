package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.GestureUtils;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class FingerGestureService extends AccessibilityService {

    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int brightnessMode = BrightnessGestureConsumer.getInstance().restoresAdaptiveBrightnessOnDisplaySleep()
                    ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    : SCREEN_BRIGHTNESS_MODE_MANUAL;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        FingerprintGestureController gestureController = getFingerprintGestureController();
        gestureController.registerFingerprintGestureCallback(GestureMapper.getInstance(), new Handler());

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {}

    @Override
    public void onInterrupt() {}

    public static void setBackgroundColor(int color) {
        GestureUtils.getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public static void setSliderColor(int color) {
        GestureUtils.getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public static void setIncrementPercentage(int incrementValue) {
        GestureUtils.getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public static void setPositionPercentage(int positionPercentage) {
        GestureUtils.getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }


    public static int getBackgroundColor() {
        return GestureUtils.getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorPrimary));
    }

    public static int getSliderColor() {
        return GestureUtils.getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorAccent));
    }

    public static int getIncrementPercentage() {
        return GestureUtils.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public static int getPositionPercentage() {
        return GestureUtils.getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }


}
