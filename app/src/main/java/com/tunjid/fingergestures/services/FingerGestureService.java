package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.GestureUtils;

public class FingerGestureService extends AccessibilityService {

    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        FingerprintGestureController gestureController = getFingerprintGestureController();
        gestureController.registerFingerprintGestureCallback(GestureMapper.getInstance(), new Handler());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

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
