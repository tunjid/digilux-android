package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.BrightnessActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Single;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToByte;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToFraction;
import static io.reactivex.Single.error;
import static io.reactivex.Single.fromCallable;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public class BrightnessGestureConsumer implements GestureConsumer {

    static final float MAX_BRIGHTNESS = 255F;
    private static final float MIN_BRIGHTNESS = 1F;
    private static final float MIN_DIM_PERCENT = 0F;
    private static final float MAX_DIM_PERCENT = 0.8F;
    private static final float DEF_DIM_PERCENT = MIN_DIM_PERCENT;
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    public static final String ACTION_SCREEN_DIMMER_CHANGED = "show screen dimmer";
    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";
    private static final String SLIDER_VISIBLE = "slider visible";
    private static final String ADAPTIVE_BRIGHTNESS = "adaptive brightness";
    private static final String SCREEN_DIMMER_ENABLED = "screen dimmer enabled";
    private static final String SCREEN_DIMMER_DIM_PERCENT = "screen dimmer dim percent";

    private final App app;
    private final Set<Integer> gestures;

    @SuppressLint("StaticFieldLeak")
    private static BrightnessGestureConsumer instance;

    public static BrightnessGestureConsumer getInstance() {
        if (instance == null) instance = new BrightnessGestureConsumer();
        return instance;
    }

    private BrightnessGestureConsumer() {
        app = App.getInstance();
        gestures = new HashSet<>();
        gestures.add(INCREASE_BRIGHTNESS);
        gestures.add(REDUCE_BRIGHTNESS);
        gestures.add(MAXIMIZE_BRIGHTNESS);
        gestures.add(MINIMIZE_BRIGHTNESS);
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
        else if (gestureAction == MINIMIZE_BRIGHTNESS || gestureAction == MAXIMIZE_BRIGHTNESS) {
            removeDimmer();
        }

        saveBrightness(byteValue);

        float brightness = byteValue / MAX_BRIGHTNESS;
        intent.putExtra(BRIGHTNESS_FRACTION, brightness);

        if (shouldShowSlider()) app.startActivity(intent);
    }


    @Override
    public Set<Integer> gestures() {
        return gestures;
    }

    public void saveBrightness(int byteValue) {
        if (!App.canWriteToSettings()) return;

        ContentResolver contentResolver = app.getContentResolver();
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }

    public void onScreenTurnedOff() {
        if (!App.canWriteToSettings()) return;

        boolean restoresAdaptiveBrightness = restoresAdaptiveBrightnessOnDisplaySleep();
        int brightnessMode = restoresAdaptiveBrightness
                ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : SCREEN_BRIGHTNESS_MODE_MANUAL;
        Settings.System.putInt(app.getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode);
        if (restoresAdaptiveBrightness) removeDimmer();
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

    public void setBackgroundColor(int color) {
        app.getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public void setSliderColor(int color) {
        app.getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public void setIncrementPercentage(int incrementValue) {
        app.getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public void setPositionPercentage(int positionPercentage) {
        app.getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }

    private void setDimmerPercent(float percentage) {
        app.getPreferences().edit().putFloat(SCREEN_DIMMER_DIM_PERCENT, percentage).apply();
    }

    public void shouldRestoreAdaptiveBrightnessOnDisplaySleep(boolean restore) {
        app.getPreferences().edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply();
    }

    public void setSliderVisible(boolean visible) {
        app.getPreferences().edit().putBoolean(SLIDER_VISIBLE, visible).apply();
    }

    public void setDimmerEnabled(boolean enabled) {
        app.getPreferences().edit().putBoolean(SCREEN_DIMMER_ENABLED, enabled).apply();
        if (!enabled) removeDimmer();
    }

    public int getBackgroundColor() {
        return app.getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(App.getInstance(), R.color.colorPrimary));
    }

    public int getSliderColor() {
        return app.getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(App.getInstance(), R.color.colorAccent));
    }

    public int getIncrementPercentage() {
        return app.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public int getPositionPercentage() {
        return app.getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }

    public float getScreenDimmerDimPercent() {
        return app.getPreferences().getFloat(SCREEN_DIMMER_DIM_PERCENT, DEF_DIM_PERCENT);
    }

    public boolean restoresAdaptiveBrightnessOnDisplaySleep() {
        return app.getPreferences().getBoolean(ADAPTIVE_BRIGHTNESS, false);
    }

    public boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(app);
    }

    public boolean isDimmerEnabled() {
        return hasOverlayPermission()
                && !PurchasesManager.getInstance().isNotPremium()
                && app.getPreferences().getBoolean(SCREEN_DIMMER_ENABLED, false);
    }

    public boolean shouldShowDimmer() {
        return getScreenDimmerDimPercent() != MIN_DIM_PERCENT;
    }

    public boolean shouldShowSlider() {
        return app.getPreferences().getBoolean(SLIDER_VISIBLE, true);
    }

    public void removeDimmer() {
        setDimmerPercent(MIN_DIM_PERCENT);
        Intent intent = new Intent(ACTION_SCREEN_DIMMER_CHANGED);
        intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, getScreenDimmerDimPercent());
        LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
    }

    public Single<Palette> extractPalette() {
        WallpaperManager wallpaperManager = app.getSystemService(WallpaperManager.class);
        Drawable drawable = wallpaperManager.getDrawable();

        if (!(drawable instanceof BitmapDrawable))
            return error(new Exception("Not a BitmapDrawable"));

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        return fromCallable(() -> Palette.from(bitmap).generate()).subscribeOn(computation()).observeOn(mainThread());
    }

    private static float roundDown(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
        return bd.floatValue();
    }
}
