package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.BrightnessActivity;

import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class FingerGestureService extends AccessibilityService {

    private static final String BRIGHTNESS_PREFS = "brightness prefs";
    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";
    private static final String HORIZONTAL_SWIPING = "horizontal swiping";

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;
    private static final float MAX_BRIGHTNESS = 255F;
    private static final float MIN_BRIGHTNESS = 1F;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        FingerprintGestureController gestureController = getFingerprintGestureController();
        gestureController.registerFingerprintGestureCallback(new BrightnessControl(), new Handler());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    public static void setBackgroundColor(int color) {
        getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public static void setSliderColor(int color) {
        getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public static void setIncrementPercentage(int incrementValue) {
        getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public static void setPositionPercentage(int positionPercentage) {
        getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }

    public static void setHorizontalSwipeEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(HORIZONTAL_SWIPING, enabled).apply();
    }

    public static int getBackgroundColor() {
        return getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorPrimary));
    }

    public static int getSliderColor() {
        return getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorAccent));
    }

    public static int getIncrementPercentage() {
        return getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public static int getPositionPercentage() {
        return getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }

    private static boolean isHorizontalSwipeEnabled() {
        return getPreferences().getBoolean(HORIZONTAL_SWIPING, false);
    }

    private static SharedPreferences getPreferences() {
        return Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
    }

    final class BrightnessControl extends FingerprintGestureController.FingerprintGestureCallback {

        @Override
        public void onGestureDetectionAvailabilityChanged(boolean available) {
            super.onGestureDetectionAvailabilityChanged(available);
        }

        @Override
        public void onGestureDetected(int gesture) {
            super.onGestureDetected(gesture);
            if (isVerticalSwipe(gesture) || isHorizontalSwipeEnabled()) adjustBrightness(gesture);
        }

        private void adjustBrightness(int gesture) {
            int byteValue;

            try {byteValue = Settings.System.getInt(getContentResolver(), SCREEN_BRIGHTNESS);}
            catch (Exception e) {byteValue = (int) MAX_BRIGHTNESS;}

            if (gesture == FINGERPRINT_GESTURE_SWIPE_UP) byteValue = increase(byteValue);
            else if (gesture == FINGERPRINT_GESTURE_SWIPE_DOWN) byteValue = reduce(byteValue);
            else if (gesture == FINGERPRINT_GESTURE_SWIPE_RIGHT) byteValue = (int) MAX_BRIGHTNESS;
            else if (gesture == FINGERPRINT_GESTURE_SWIPE_LEFT) byteValue = (int) MIN_BRIGHTNESS;

            saveBrightness(byteValue);

            float brightness = byteValue / MAX_BRIGHTNESS;

            Intent intent = new Intent(FingerGestureService.this, BrightnessActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(BRIGHTNESS_FRACTION, brightness);
            startActivity(intent);
        }

        private boolean isVerticalSwipe(int gesture) {
            return gesture == FINGERPRINT_GESTURE_SWIPE_UP || gesture == FINGERPRINT_GESTURE_SWIPE_DOWN;
        }

        private int reduce(int byteValue) {
            return Math.max(byteValue - normalizePercetageToByte(getIncrementPercentage()), (int) MIN_BRIGHTNESS);
        }

        private int increase(int byteValue) {
            return Math.min(byteValue + normalizePercetageToByte(getIncrementPercentage()), (int) MAX_BRIGHTNESS);
        }
    }

    public static int normalizePercetageToByte(int percentage) {
        return (int) (MAX_BRIGHTNESS * (percentage / 100F));
    }

    public static void saveBrightness(int byteValue) {
        ContentResolver contentResolver = Application.getContext().getContentResolver();
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }
}
