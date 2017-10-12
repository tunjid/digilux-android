package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.BrightnessActivity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static com.tunjid.fingergestures.Application.getContext;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.INCREASE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MAXIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MININIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.REDUCE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.getPreferences;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToByte;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToFraction;

public class BrightnessGestureConsumer implements GestureConsumer {

    static final float MAX_BRIGHTNESS = 255F;
    private static final float MIN_BRIGHTNESS = 1F;
    private static final float MIN_DIM_PERCENT = 0F;
    private static final float MAX_DIM_PERCENT = 0.8F;
    private static final float DEF_DIM_PERCENT = MIN_DIM_PERCENT;
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    public static final String ACTION_SCREEN_FILTER_CHANGED = "show screen filter";
    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";
    private static final String ADAPTIVE_BRIGHTNESS = "adaptive brightness";
    private static final String SCREEN_FILTER_ENABLED = "screen filter enabled";
    private static final String SCREEN_FILTER_DIM_PERCENT = "screen filter dim percent";

    private final Context app;
    private final Set<Integer> gestures;

    @SuppressLint("StaticFieldLeak")
    private static BrightnessGestureConsumer instance;

    public static BrightnessGestureConsumer getInstance() {
        if (instance == null) instance = new BrightnessGestureConsumer();
        return instance;
    }

    private BrightnessGestureConsumer() {
        app = getContext();
        gestures = new HashSet<>();
        gestures.add(INCREASE_BRIGHTNESS);
        gestures.add(REDUCE_BRIGHTNESS);
        gestures.add(MAXIMIZE_BRIGHTNESS);
        gestures.add(MININIMIZE_BRIGHTNESS);
    }

    @Override
    public void onGestureActionTriggered(@GestureUtils.GestureAction int gestureAction) {
        int byteValue;
        int originalValue;

        try {byteValue = Settings.System.getInt(app.getContentResolver(), SCREEN_BRIGHTNESS);}
        catch (Exception e) {byteValue = (int) MAX_BRIGHTNESS;}

        originalValue = byteValue;

        if (gestureAction == INCREASE_BRIGHTNESS) byteValue = increase(byteValue);
        else if (gestureAction == REDUCE_BRIGHTNESS) byteValue = reduce(byteValue);
        else if (gestureAction == MAXIMIZE_BRIGHTNESS) byteValue = (int) MAX_BRIGHTNESS;
        else if (gestureAction == MININIMIZE_BRIGHTNESS) byteValue = (int) MIN_BRIGHTNESS;

        Intent intent = new Intent(app, BrightnessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (engagedFilter(gestureAction, originalValue)) {
            byteValue = originalValue;
            intent.setAction(ACTION_SCREEN_FILTER_CHANGED);
            intent.putExtra(SCREEN_FILTER_DIM_PERCENT, getScreenFilterDimPercent());
            LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
        }

        saveBrightness(byteValue);

        float brightness = byteValue / MAX_BRIGHTNESS;
        intent.putExtra(BRIGHTNESS_FRACTION, brightness);

        app.startActivity(intent);
    }


    @Override
    public Set<Integer> gestures() {
        return gestures;
    }

    public void saveBrightness(int byteValue) {
        ContentResolver contentResolver = app.getContentResolver();
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }

    public void onScreenTurnedOff() {
        int brightnessMode = restoresAdaptiveBrightnessOnDisplaySleep()
                ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : SCREEN_BRIGHTNESS_MODE_MANUAL;
        Settings.System.putInt(app.getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode);
    }

    private boolean engagedFilter(@GestureUtils.GestureAction int gestureAction, int byteValue) {
        if (!isFilterEnabled()) return false;
        if (byteValue == (int) MIN_BRIGHTNESS && gestureAction == REDUCE_BRIGHTNESS) {
            increaseScreenFilter();
            return true;
        }
        else if (gestureAction == INCREASE_BRIGHTNESS && getScreenFilterDimPercent() > MIN_DIM_PERCENT) {
            reduceScreenFilter();
            return true;
        }
        return false;
    }

    private void reduceScreenFilter() {
        float current = getScreenFilterDimPercent();
        float changed = current - normalizePercetageToFraction(getIncrementPercentage());
        setFilterDimPercent(Math.max(roundDown(changed), MIN_DIM_PERCENT));
    }

    private void increaseScreenFilter() {
        float current = getScreenFilterDimPercent();
        float changed = current + normalizePercetageToFraction(getIncrementPercentage());
        setFilterDimPercent(Math.min(roundDown(changed), MAX_DIM_PERCENT));
    }

    private int reduce(int byteValue) {
        return Math.max(byteValue - normalizePercetageToByte(getIncrementPercentage()), (int) MIN_BRIGHTNESS);
    }

    private int increase(int byteValue) {
        return Math.min(byteValue + normalizePercetageToByte(getIncrementPercentage()), (int) MAX_BRIGHTNESS);
    }

    public void setBackgroundColor(int color) {
        getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public void setSliderColor(int color) {
        getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public void setIncrementPercentage(int incrementValue) {
        getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public void setPositionPercentage(int positionPercentage) {
        getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }

    private void setFilterDimPercent(float percentage) {
        getPreferences().edit().putFloat(SCREEN_FILTER_DIM_PERCENT, percentage).apply();
    }

    public void shouldRestoreAdaptiveBrightnessOnDisplaySleep(boolean restore) {
        getPreferences().edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply();
    }

    public void setFilterEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(SCREEN_FILTER_ENABLED, enabled).apply();
    }

    public int getBackgroundColor() {
        return getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(getContext(), R.color.colorPrimary));
    }

    public int getSliderColor() {
        return getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    public int getIncrementPercentage() {
        return getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public int getPositionPercentage() {
        return getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }

    public float getScreenFilterDimPercent() {
        return getPreferences().getFloat(SCREEN_FILTER_DIM_PERCENT, DEF_DIM_PERCENT);
    }

    public boolean restoresAdaptiveBrightnessOnDisplaySleep() {
        return getPreferences().getBoolean(ADAPTIVE_BRIGHTNESS, false);
    }

    public boolean hasFilterPermission() {
        return Settings.canDrawOverlays(app);
    }

    public boolean isFilterEnabled() {
        return hasFilterPermission() && getPreferences().getBoolean(SCREEN_FILTER_ENABLED, false);
    }

    public boolean shouldShowFilter() {
        return getScreenFilterDimPercent() != MIN_DIM_PERCENT;
    }

    public void removeFilter() {
        setFilterDimPercent(MIN_DIM_PERCENT);
        Intent intent = new Intent(ACTION_SCREEN_FILTER_CHANGED);
        intent.putExtra(SCREEN_FILTER_DIM_PERCENT, getScreenFilterDimPercent());
        LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
    }

    private static float roundDown(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
        return bd.floatValue();
    }
}
