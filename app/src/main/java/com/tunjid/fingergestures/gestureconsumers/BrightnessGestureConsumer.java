package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.BrightnessActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.math.BigDecimal;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToByte;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToFraction;

public class BrightnessGestureConsumer implements GestureConsumer {

    static final float MAX_BRIGHTNESS = 255F;
    private static final float MIN_BRIGHTNESS = 0F;
    private static final float MIN_DIM_PERCENT = 0F;
    private static final float MAX_DIM_PERCENT = 0.8F;
    private static final float DEF_DIM_PERCENT = MIN_DIM_PERCENT;
    private static final int ZERO_PERCENT = 0;
    private static final int MAX_SLIDER_DURATION = 5000;
    private static final int MAX_ADAPTIVE_THRESHOLD = 1200;
    private static final int HUNDRED_PERCENT = 100;
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;
    private static final int DEF_SLIDER_DURATION_PERCENT = 60;
    private static final int DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD = 50;


    public static final String BRIGHTNESS_FRACTION = "brightness value";
    public static final String ACTION_SCREEN_DIMMER_CHANGED = "show screen dimmer";
    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";
    private static final String SLIDER_DURATION = "slider duration";
    private static final String SLIDER_VISIBLE = "slider visible";
    private static final String ADAPTIVE_BRIGHTNESS = "adaptive brightness";
    private static final String ADAPTIVE_BRIGHTNESS_THRESHOLD = "adaptive brightness threshold";
    private static final String SCREEN_DIMMER_ENABLED = "screen dimmer enabled";
    private static final String SCREEN_DIMMER_DIM_PERCENT = "screen dimmer dim percent";
    private static final String ANIMATES_SLIDER = "animates slider";

    private final App app;

    @SuppressLint("StaticFieldLeak")
    private static BrightnessGestureConsumer instance;

    public static BrightnessGestureConsumer getInstance() {
        if (instance == null) instance = new BrightnessGestureConsumer();
        return instance;
    }

    private BrightnessGestureConsumer() {
        app = App.getInstance();
    }

    @Override
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        int byteValue;
        int originalValue;

        try {byteValue = Settings.System.getInt(app.getContentResolver(), SCREEN_BRIGHTNESS);}
        catch (Exception e) {byteValue = (int) MAX_BRIGHTNESS;}

        originalValue = byteValue;

        if (gestureAction == INCREASE_BRIGHTNESS) byteValue = increase(byteValue);
        else if (gestureAction == REDUCE_BRIGHTNESS) byteValue = reduce(byteValue);
        else if (gestureAction == MAXIMIZE_BRIGHTNESS) byteValue = (int) MAX_BRIGHTNESS;
        else if (gestureAction == MINIMIZE_BRIGHTNESS) byteValue = (int) MIN_BRIGHTNESS;

        Intent intent = new Intent(app, BrightnessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (engagedDimmer(gestureAction, originalValue)) {
            byteValue = originalValue;
            intent.setAction(ACTION_SCREEN_DIMMER_CHANGED);
            intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, getScreenDimmerDimPercent());
            LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
        }
        else if (shouldRemoveDimmerOnChange(gestureAction)) {
            removeDimmer();
        }

        saveBrightness(byteValue);

        float brightness = byteValue / MAX_BRIGHTNESS;
        intent.putExtra(BRIGHTNESS_FRACTION, brightness);

        if (shouldShowSlider()) app.startActivity(intent);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        switch (gesture) {
            case INCREASE_BRIGHTNESS:
            case REDUCE_BRIGHTNESS:
            case MAXIMIZE_BRIGHTNESS:
            case MINIMIZE_BRIGHTNESS:
                return true;
            default:
                return false;
        }
    }

    public void saveBrightness(int byteValue) {
        if (!App.canWriteToSettings()) return;

        ContentResolver contentResolver = app.getContentResolver();
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }

    public void onScreenTurnedOn() {
        if (!App.canWriteToSettings()) return;

        final int threshold = adaptiveThresholdToLux(getAdaptiveBrightnessThreshold());
        final boolean restoresAdaptiveBrightness = restoresAdaptiveBrightnessOnDisplaySleep();
        final boolean toggleAndLeave = restoresAdaptiveBrightness && PurchasesManager.getInstance().isNotPremium();

        if (!restoresAdaptiveBrightness) return;

        if (threshold == 0 || toggleAndLeave) {
            toggleAdaptiveBrightness(true);
            return;
        }

        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;

        final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null) return;

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                sensorManager.unregisterListener(this, lightSensor);

                if (!restoresAdaptiveBrightness) return;

                float[] values = event.values;
                float lux = values != null && values.length > 0 ? values[0] : -1;
                boolean restoredBrightness = lux >= threshold;

                toggleAdaptiveBrightness(restoredBrightness);
                if (restoredBrightness) removeDimmer();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, lightSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private boolean engagedDimmer(@GestureAction int gestureAction, int byteValue) {
        if (!isDimmerEnabled()) return false;
        if (byteValue == (int) MIN_BRIGHTNESS && gestureAction == REDUCE_BRIGHTNESS) {
            increaseScreenDimmer();
            return true;
        }
        else if (gestureAction == INCREASE_BRIGHTNESS && getScreenDimmerDimPercent() > MIN_DIM_PERCENT) {
            reduceScreenDimmer();
            return true;
        }
        return false;
    }

    private void reduceScreenDimmer() {
        float current = getScreenDimmerDimPercent();
        float changed = current - normalizePercetageToFraction(getIncrementPercentage());
        setDimmerPercent(Math.max(roundDown(changed), MIN_DIM_PERCENT));
    }

    private void increaseScreenDimmer() {
        float current = getScreenDimmerDimPercent();
        float changed = current + normalizePercetageToFraction(getIncrementPercentage());
        setDimmerPercent(Math.min(roundDown(changed), MAX_DIM_PERCENT));
    }

    private int reduce(int byteValue) {
        return Math.max(byteValue - normalizePercetageToByte(getIncrementPercentage()), (int) MIN_BRIGHTNESS);
    }

    private int increase(int byteValue) {
        return Math.min(byteValue + normalizePercetageToByte(getIncrementPercentage()), (int) MAX_BRIGHTNESS);
    }

    public void setBackgroundColor(@ColorInt int color) {
        app.getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public void setSliderColor(@ColorInt int color) {
        app.getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public void setSliderDurationPercentage(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int duration) {
        app.getPreferences().edit().putInt(SLIDER_DURATION, duration).apply();
    }

    public void setIncrementPercentage(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int incrementValue) {
        app.getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public void setPositionPercentage(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int positionPercentage) {
        app.getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }

    private void setDimmerPercent(@FloatRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) float percentage) {
        app.getPreferences().edit().putFloat(SCREEN_DIMMER_DIM_PERCENT, percentage).apply();
    }

    public void setAdaptiveBrightnessThreshold(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int threshold) {
        app.getPreferences().edit().putInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, threshold).apply();
    }

    public void shouldRestoreAdaptiveBrightnessOnDisplaySleep(boolean restore) {
        app.getPreferences().edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply();
    }

    public void setSliderVisible(boolean visible) {
        app.getPreferences().edit().putBoolean(SLIDER_VISIBLE, visible).apply();
    }

    public void setAnimatesSlider(boolean visible) {
        app.getPreferences().edit().putBoolean(ANIMATES_SLIDER, visible).apply();
    }

    public void setDimmerEnabled(boolean enabled) {
        app.getPreferences().edit().putBoolean(SCREEN_DIMMER_ENABLED, enabled).apply();
        if (!enabled) removeDimmer();
    }

    @ColorInt
    public int getBackgroundColor() {
        return app.getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(App.getInstance(), R.color.colorPrimary));
    }

    @ColorInt
    public int getSliderColor() {
        return app.getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(App.getInstance(), R.color.colorAccent));
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getIncrementPercentage() {
        return app.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getPositionPercentage() {
        return app.getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getSliderDurationPercentage() {
        return app.getPreferences().getInt(SLIDER_DURATION, DEF_SLIDER_DURATION_PERCENT);
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getAdaptiveBrightnessThreshold() {
        return app.getPreferences().getInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD);
    }

    public int getSliderDurationMillis() {
        return durationPercentageToMillis(getSliderDurationPercentage());
    }

    public float getScreenDimmerDimPercent() {
        return app.getPreferences().getFloat(SCREEN_DIMMER_DIM_PERCENT, DEF_DIM_PERCENT);
    }

    public boolean restoresAdaptiveBrightnessOnDisplaySleep() {
        return app.getPreferences().getBoolean(ADAPTIVE_BRIGHTNESS, false);
    }

    public boolean supportsAmbientThreshold() {
        return PurchasesManager.getInstance().isPremium()
                && restoresAdaptiveBrightnessOnDisplaySleep()
                && hasBrightnessSensor();
    }

    public boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(app);
    }

    public boolean isDimmerEnabled() {
        return hasOverlayPermission()
                && PurchasesManager.getInstance().isPremium()
                && app.getPreferences().getBoolean(SCREEN_DIMMER_ENABLED, false);
    }

    public boolean shouldShowDimmer() {
        return getScreenDimmerDimPercent() != MIN_DIM_PERCENT;
    }

    public boolean shouldShowSlider() {
        return app.getPreferences().getBoolean(SLIDER_VISIBLE, true);
    }

    public boolean shouldAnimateSlider() {
        return app.getPreferences().getBoolean(ANIMATES_SLIDER, true);
    }

    public String getSliderDurationText(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int duration) {
        int millis = durationPercentageToMillis(duration);
        float seconds = millis / 1000F;
        return app.getString(R.string.duration_value, seconds);
    }

    public String getAdaptiveBrightnessThresholdText(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int percent) {
        if (!hasBrightnessSensor())
            return app.getString(R.string.unavailable_brightness_sensor);

        if (PurchasesManager.getInstance().isNotPremium())
            return app.getString(R.string.go_premium_text);

        if (!restoresAdaptiveBrightnessOnDisplaySleep())
            return (app.getString(R.string.adjust_adaptive_threshold_prompt));

        int lux = adaptiveThresholdToLux(percent);
        String descriptor = app.getString(lux < 50
                ? R.string.adaptive_threshold_low
                : lux < 1000 ? R.string.adaptive_threshold_medium
                : R.string.adaptive_threshold_high);

        return app.getString(R.string.adaptive_threshold, lux, descriptor);
    }

    public void removeDimmer() {
        setDimmerPercent(MIN_DIM_PERCENT);
        Intent intent = new Intent(ACTION_SCREEN_DIMMER_CHANGED);
        intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, getScreenDimmerDimPercent());
        LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
    }

    private int durationPercentageToMillis(int percentage) {
        return (int) (percentage * MAX_SLIDER_DURATION / 100F);
    }

    private int adaptiveThresholdToLux(int percentage) {
        return (int) (percentage * MAX_ADAPTIVE_THRESHOLD / 100F);
    }

    private void toggleAdaptiveBrightness(boolean on) {
        int brightnessMode = on
                ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : SCREEN_BRIGHTNESS_MODE_MANUAL;
        Settings.System.putInt(app.getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode);
    }

    private static float roundDown(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
        return bd.floatValue();
    }

    private boolean hasBrightnessSensor() {
        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return false;

        final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        return lightSensor != null;
    }

    private boolean shouldRemoveDimmerOnChange(@GestureAction int gestureAction) {
        return gestureAction == MINIMIZE_BRIGHTNESS || gestureAction == MAXIMIZE_BRIGHTNESS
                || (shouldShowDimmer() && PurchasesManager.getInstance().isNotPremium());
    }
}
