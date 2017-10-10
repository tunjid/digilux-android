package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.activities.BrightnessActivity;

import java.util.HashSet;
import java.util.Set;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.INCREASE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MAXIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MININIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.REDUCE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.getPreferences;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToByte;
import static com.tunjid.fingergestures.services.FingerGestureService.BRIGHTNESS_FRACTION;
import static com.tunjid.fingergestures.services.FingerGestureService.getIncrementPercentage;

public class BrightnessGestureConsumer implements GestureConsumer {

    static final float MAX_BRIGHTNESS = 255F;
    private static final float MIN_BRIGHTNESS = 1F;

    private static final String ADAPTIVE_BRIGHTNESS = "adaptive brightness";

    private final Context app;
    private final Set<Integer> gestures;

    @SuppressLint("StaticFieldLeak")
    private static BrightnessGestureConsumer instance;

    public static BrightnessGestureConsumer getInstance() {
        if (instance == null) instance = new BrightnessGestureConsumer();
        return instance;
    }

    private BrightnessGestureConsumer() {
        app = Application.getContext();
        gestures = new HashSet<>();
        gestures.add(INCREASE_BRIGHTNESS);
        gestures.add(REDUCE_BRIGHTNESS);
        gestures.add(MAXIMIZE_BRIGHTNESS);
        gestures.add(MININIMIZE_BRIGHTNESS);
    }

    @Override
    public void onGestureActionTriggered(@GestureUtils.GestureAction int gestureAction) {
        int byteValue;

        try {byteValue = Settings.System.getInt(app.getContentResolver(), SCREEN_BRIGHTNESS);}
        catch (Exception e) {byteValue = (int) MAX_BRIGHTNESS;}

        if (gestureAction == INCREASE_BRIGHTNESS) byteValue = increase(byteValue);
        else if (gestureAction == REDUCE_BRIGHTNESS) byteValue = reduce(byteValue);
        else if (gestureAction == MAXIMIZE_BRIGHTNESS) byteValue = (int) MAX_BRIGHTNESS;
        else if (gestureAction == MININIMIZE_BRIGHTNESS) byteValue = (int) MIN_BRIGHTNESS;

        saveBrightness(byteValue);
        float brightness = byteValue / MAX_BRIGHTNESS;

        Intent intent = new Intent(app, BrightnessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    private int reduce(int byteValue) {
        return Math.max(byteValue - normalizePercetageToByte(getIncrementPercentage()), (int) MIN_BRIGHTNESS);
    }

    private int increase(int byteValue) {
        return Math.min(byteValue + normalizePercetageToByte(getIncrementPercentage()), (int) MAX_BRIGHTNESS);
    }

    public void shouldRestoreAdaptiveBrightnessOnDisplaySleep(boolean restore) {
        getPreferences().edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply();
    }

    public boolean restoresAdaptiveBrightnessOnDisplaySleep() {
        return getPreferences().getBoolean(ADAPTIVE_BRIGHTNESS, false);
    }
}
