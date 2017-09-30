package com.tunjid.fingergestures;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class FingerGestureService extends AccessibilityService {

    public static final String BRIGHTNESS_FRACTION = "brightness value";

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

    final class BrightnessControl extends FingerprintGestureController.FingerprintGestureCallback {

        private static final float MAX_BRIGHTNESS = 255F;
        private static final float MIN_BRIGHTNESS = 1F;

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

            saveBrightness(byteValue, getContentResolver());

            float brightness = byteValue / MAX_BRIGHTNESS;

            Intent intent = new Intent(FingerGestureService.this, BrightnessActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(BRIGHTNESS_FRACTION, brightness);
            startActivity(intent);
        }

        private int reduce(int byteValue) {
            return Math.max(byteValue - 20, (int) MIN_BRIGHTNESS);
        }

        private int increase(int byteValue) {
            return Math.min(byteValue + 20, (int) MAX_BRIGHTNESS);
        }
    }

    public static void saveBrightness(int byteValue, ContentResolver contentResolver) {
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }
}
