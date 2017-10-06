package com.tunjid.fingergestures;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;

import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN;
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
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        preferences.edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public static void setSliderColor(int color) {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        preferences.edit().putInt(SLIDER_COLOR, color).apply();
    }

    public static void setIncrementPercentage(int incrementValue) {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        preferences.edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public static void setPositionPercentage(int positionPercentage) {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        preferences.edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }

    public static int getBackgroundColor() {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        return preferences.getInt(BACKGROUND_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorPrimary));
    }

    public static int getSliderColor() {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        return preferences.getInt(SLIDER_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorAccent));
    }

    public static int getIncrementPercentage() {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        return preferences.getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public static int getPositionPercentage() {
        SharedPreferences preferences = Application.getContext().getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
        return preferences.getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }

    final class BrightnessControl extends FingerprintGestureController.FingerprintGestureCallback {


        @Override
        public void onGestureDetectionAvailabilityChanged(boolean available) {
            super.onGestureDetectionAvailabilityChanged(available);
        }

        @Override
        public void onGestureDetected(int gesture) {
            super.onGestureDetected(gesture);

            if (gesture == FINGERPRINT_GESTURE_SWIPE_UP || gesture == FINGERPRINT_GESTURE_SWIPE_DOWN) {
                adjustBrightness(gesture);
            }
        }

        private void adjustBrightness(int gesture) {
            int byteValue;

            try {byteValue = Settings.System.getInt(getContentResolver(), SCREEN_BRIGHTNESS);}
            catch (Exception e) {byteValue = (int) MAX_BRIGHTNESS;}

            if (gesture == FINGERPRINT_GESTURE_SWIPE_UP) byteValue = increase(byteValue);
            else if (gesture == FINGERPRINT_GESTURE_SWIPE_DOWN) byteValue = reduce(byteValue);

            saveBrightness(byteValue);

            float brightness = byteValue / MAX_BRIGHTNESS;

            Intent intent = new Intent(FingerGestureService.this, BrightnessActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(BRIGHTNESS_FRACTION, brightness);
            startActivity(intent);
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
