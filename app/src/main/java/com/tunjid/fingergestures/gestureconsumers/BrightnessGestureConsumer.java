package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BrightnessLookup;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.SetManager;
import com.tunjid.fingergestures.activities.BrightnessActivity;
import com.tunjid.fingergestures.billing.PurchasesVerifier;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static com.tunjid.fingergestures.App.transformApp;
import static com.tunjid.fingergestures.App.withApp;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.normalizePercentageToFraction;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;

public class BrightnessGestureConsumer implements GestureConsumer {

    private static final int FUZZ_THRESHOLD = 15;
    private static final float MIN_BRIGHTNESS = 0F;
    private static final float MIN_DIM_PERCENT = 0F;
    private static final float MAX_DIM_PERCENT = .8F;
    private static final float MAX_BRIGHTNESS = 255F;
    private static final float DEF_DIM_PERCENT = MIN_DIM_PERCENT;
    private static final int MAX_ADAPTIVE_THRESHOLD = 1200;
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;
    private static final int DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD = 50;

    public static final String CURRENT_BRIGHTNESS_BYTE = "brightness value";
    public static final String ACTION_SCREEN_DIMMER_CHANGED = "show screen dimmer";
    private static final String INCREMENT_VALUE = "increment value";
    private static final String SLIDER_POSITION = "slider position";
    private static final String SLIDER_VISIBLE = "slider visible";
    private static final String LOGARITHMIC_SCALE = "logarithmic scale";
    private static final String ADAPTIVE_BRIGHTNESS = "adaptive brightness";
    private static final String ADAPTIVE_BRIGHTNESS_THRESHOLD = "adaptive brightness threshold";
    private static final String SCREEN_DIMMER_ENABLED = "screen dimmer enabled";
    private static final String SCREEN_DIMMER_DIM_PERCENT = "screen dimmer dim percent";
    private static final String ANIMATES_SLIDER = "animates slider";
    private static final String DISCRETE_BRIGHTNESS_SET = "discrete brightness values";
    private static final String EMPTY_STRING = "";

    private SetManager<Integer> discreteBrightnessManager;

    @SuppressLint("StaticFieldLeak")
    private static BrightnessGestureConsumer instance;

    public static BrightnessGestureConsumer getInstance() {
        if (instance == null) instance = new BrightnessGestureConsumer();
        return instance;
    }

    private BrightnessGestureConsumer() {
        discreteBrightnessManager = new SetManager<>(Integer::compareTo, ignored -> true, Integer::valueOf, String::valueOf);
    }

    @Override
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        int byteValue;
        int originalValue;

        App app = App.getInstance();
        if (app == null) return;

        try { byteValue = Settings.System.getInt(app.getContentResolver(), SCREEN_BRIGHTNESS); }
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
            app.broadcast(intent);
        }
        else if (shouldRemoveDimmerOnChange(gestureAction)) {
            removeDimmer();
        }

        saveBrightness(byteValue);

        intent.putExtra(CURRENT_BRIGHTNESS_BYTE, byteValue);

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

        ContentResolver contentResolver = transformApp(ContextWrapper::getContentResolver);
        if (contentResolver == null) return;

        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue);
    }

    public void onScreenTurnedOn() {
        if (!App.canWriteToSettings()) return;

        final int threshold = adaptiveThresholdToLux(getAdaptiveBrightnessThreshold());
        final boolean restoresAdaptiveBrightness = restoresAdaptiveBrightnessOnDisplaySleep();
        final boolean toggleAndLeave = restoresAdaptiveBrightness && PurchasesVerifier.getInstance().isNotPremium();

        if (!restoresAdaptiveBrightness) return;

        if (threshold == 0 || toggleAndLeave) {
            toggleAdaptiveBrightness(true);
            return;
        }

        SensorManager sensorManager = transformApp(app -> (SensorManager) app.getSystemService(Context.SENSOR_SERVICE));
        if (sensorManager == null) return;

        final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null) return;

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                sensorManager.unregisterListener(this, lightSensor);

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
        float changed = current - normalizePercentageToFraction(getIncrementPercentage());
        setDimmerPercent(Math.max(roundDown(changed), MIN_DIM_PERCENT));
    }

    private void increaseScreenDimmer() {
        float current = getScreenDimmerDimPercent();
        float changed = current + normalizePercentageToFraction(getIncrementPercentage());
        setDimmerPercent(Math.min(roundDown(changed), MAX_DIM_PERCENT));
    }

    private int reduce(int byteValue) {
        return noDiscreteBrightness()
                ? Math.max(adjustByte(byteValue, false), (int) MIN_BRIGHTNESS)
                : findDiscreteBrightnessValue(byteValue, false);
    }

    private int increase(int byteValue) {
        return noDiscreteBrightness()
                ? Math.min(adjustByte(byteValue, true), (int) MAX_BRIGHTNESS)
                : findDiscreteBrightnessValue(byteValue, true);
    }

    private int adjustByte(int byteValue, boolean increase) {
        int delta = getIncrementPercentage();
        int asPercentage = byteToPercentage(byteValue);
        if (increase) asPercentage = Math.min(asPercentage + delta, 100);
        else asPercentage = Math.max(asPercentage - delta, 0);

        return percentToByte(asPercentage);
    }

    public void setIncrementPercentage(@IntRange(from = GestureConsumer.ZERO_PERCENT,
            to = GestureConsumer.HUNDRED_PERCENT) int incrementValue) {
        withApp(app -> app.getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply());
    }

    public void setPositionPercentage(@IntRange(from = GestureConsumer.ZERO_PERCENT,
            to = GestureConsumer.HUNDRED_PERCENT) int positionPercentage) {
        withApp(app -> app.getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply());
    }

    private void setDimmerPercent(@FloatRange(from = GestureConsumer.ZERO_PERCENT,
            to = GestureConsumer.HUNDRED_PERCENT) float percentage) {
        withApp(app -> app.getPreferences().edit().putFloat(SCREEN_DIMMER_DIM_PERCENT, percentage).apply());
    }

    public void setAdaptiveBrightnessThreshold(@IntRange(from = GestureConsumer.ZERO_PERCENT,
            to = GestureConsumer.HUNDRED_PERCENT) int threshold) {
        withApp(app -> app.getPreferences().edit().putInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, threshold).apply());
    }

    public void shouldRestoreAdaptiveBrightnessOnDisplaySleep(boolean restore) {
        withApp(app -> app.getPreferences().edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply());
    }

    public void shouldUseLogarithmicScale(boolean isLogarithmic) {
        withApp(app -> app.getPreferences().edit().putBoolean(LOGARITHMIC_SCALE, isLogarithmic).apply());
    }

    public void setSliderVisible(boolean visible) {
        withApp(app -> app.getPreferences().edit().putBoolean(SLIDER_VISIBLE, visible).apply());
    }

    public void setAnimatesSlider(boolean visible) {
        withApp(app -> app.getPreferences().edit().putBoolean(ANIMATES_SLIDER, visible).apply());
    }

    public void setDimmerEnabled(boolean enabled) {
        withApp(app -> app.getPreferences().edit().putBoolean(SCREEN_DIMMER_ENABLED, enabled).apply());
        if (!enabled) removeDimmer();
    }

    @IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT)
    public int getIncrementPercentage() {
        return transformApp(app -> app.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE), DEF_INCREMENT_VALUE);
    }

    @IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT)
    public int getPositionPercentage() {
        return transformApp(app -> app.getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE), DEF_POSITION_VALUE);
    }

    @IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT)
    public int getAdaptiveBrightnessThreshold() {
        return transformApp(app -> app.getPreferences().getInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD), DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD);
    }

    public float getScreenDimmerDimPercent() {
        return transformApp(app -> app.getPreferences().getFloat(SCREEN_DIMMER_DIM_PERCENT, DEF_DIM_PERCENT), DEF_DIM_PERCENT);
    }

    public boolean restoresAdaptiveBrightnessOnDisplaySleep() {
        return transformApp(app -> app.getPreferences().getBoolean(ADAPTIVE_BRIGHTNESS, false), false);
    }

    public boolean usesLogarithmicScale() {
        return transformApp(app -> app.getPreferences().getBoolean(LOGARITHMIC_SCALE, App.isPieOrHigher()), App.isPieOrHigher());
    }

    public boolean supportsAmbientThreshold() {
        return PurchasesVerifier.getInstance().isPremium()
                && restoresAdaptiveBrightnessOnDisplaySleep()
                && hasBrightnessSensor();
    }

    public boolean hasOverlayPermission() {
        return transformApp(Settings::canDrawOverlays, false);
    }

    public boolean isDimmerEnabled() {
        return hasOverlayPermission()
                && PurchasesVerifier.getInstance().isPremium()
                && transformApp(app -> app.getPreferences().getBoolean(SCREEN_DIMMER_ENABLED, false), false);
    }

    public boolean shouldShowDimmer() {
        return getScreenDimmerDimPercent() != MIN_DIM_PERCENT;
    }

    public boolean shouldShowSlider() {
        return transformApp(app -> app.getPreferences().getBoolean(SLIDER_VISIBLE, true), true);
    }

    public boolean shouldAnimateSlider() {
        return transformApp(app -> app.getPreferences().getBoolean(ANIMATES_SLIDER, true), true);
    }

    public boolean canAdjustDelta() {
        return noDiscreteBrightness() || PurchasesVerifier.getInstance().isPremium();
    }

    public void addDiscreteBrightnessValue(String discreteValue) {
        discreteBrightnessManager.addToSet(discreteValue, DISCRETE_BRIGHTNESS_SET);
    }

    public void removeDiscreteBrightnessValue(String discreteValue) {
        discreteBrightnessManager.removeFromSet(discreteValue, DISCRETE_BRIGHTNESS_SET);
    }

    public String getAdjustDeltaText(int percentage) {
        return transformApp(app -> PurchasesVerifier.getInstance().isPremium() && !noDiscreteBrightness()
                ? app.getString(R.string.delta_percent_premium, percentage)
                : app.getString(R.string.delta_percent, percentage), EMPTY_STRING);
    }

    public List<String> getDiscreteBrightnessValues() {
        return discreteBrightnessManager.getList(DISCRETE_BRIGHTNESS_SET);
    }

    public String getAdaptiveBrightnessThresholdText(@IntRange(from = GestureConsumer.ZERO_PERCENT,
            to = GestureConsumer.HUNDRED_PERCENT) int percent) {
        if (!hasBrightnessSensor())
            return transformApp(app -> app.getString(R.string.unavailable_brightness_sensor), EMPTY_STRING);

        if (PurchasesVerifier.getInstance().isNotPremium())
            return transformApp(app -> app.getString(R.string.go_premium_text), EMPTY_STRING);

        if (!restoresAdaptiveBrightnessOnDisplaySleep())
            return transformApp(app -> app.getString(R.string.adjust_adaptive_threshold_prompt), EMPTY_STRING);

        int lux = adaptiveThresholdToLux(percent);

        return transformApp(app -> {
            String descriptor = app.getString(lux < 50
                    ? R.string.adaptive_threshold_low
                    : lux < 1000 ? R.string.adaptive_threshold_medium
                    : R.string.adaptive_threshold_high);

            return app.getString(R.string.adaptive_threshold, lux, descriptor);
        }, EMPTY_STRING);
    }

    public void removeDimmer() {
        setDimmerPercent(MIN_DIM_PERCENT);
        Intent intent = new Intent(ACTION_SCREEN_DIMMER_CHANGED);
        intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, getScreenDimmerDimPercent());
        withApp(app -> app.broadcast(intent));
    }

    public int percentToByte(int percentage) {
        return usesLogarithmicScale()
                ? BrightnessLookup.lookup(percentage, false)
                : (int) (percentage * MAX_BRIGHTNESS / 100);
    }

    public int byteToPercentage(int byteValue) {
        return usesLogarithmicScale()
                ? BrightnessLookup.lookup(byteValue, true)
                : (int) (byteValue * 100 / MAX_BRIGHTNESS);
    }

    private int adaptiveThresholdToLux(int percentage) {
        return (int) (percentage * MAX_ADAPTIVE_THRESHOLD / 100F);
    }

    private int findDiscreteBrightnessValue(int byteValue, boolean increasing) {
        int evaluated = byteValue < FUZZ_THRESHOLD ? byteValue : increasing ? byteValue + 2 : byteValue - 2;
        int alternative = (int) (increasing ? MAX_BRIGHTNESS : MIN_BRIGHTNESS);
        Comparator<Integer> comparator = increasing ? naturalOrder() : reverseOrder();
        Function<Integer, Boolean> filter = integer -> increasing ? integer > evaluated : integer < evaluated;

        return discreteBrightnessManager.getSet(DISCRETE_BRIGHTNESS_SET).stream()
                .map(this::stringPercentageToBrightnessInt)
                .filter(filter::apply).min(comparator).orElse(alternative);
    }

    private void toggleAdaptiveBrightness(boolean on) {
        int brightnessMode = on
                ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : SCREEN_BRIGHTNESS_MODE_MANUAL;
        withApp(app -> Settings.System.putInt(app.getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode));
    }

    private static float roundDown(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
        return bd.floatValue();
    }

    private int stringPercentageToBrightnessInt(String stringPercent) {
        Float percentage = Float.valueOf(stringPercent);
        return percentToByte(percentage.intValue());
    }

    private boolean hasBrightnessSensor() {
        SensorManager sensorManager = transformApp(app -> (SensorManager) app.getSystemService(Context.SENSOR_SERVICE));
        if (sensorManager == null) return false;

        final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        return lightSensor != null;
    }

    private boolean shouldRemoveDimmerOnChange(@GestureAction int gestureAction) {
        return gestureAction == MINIMIZE_BRIGHTNESS || gestureAction == MAXIMIZE_BRIGHTNESS
                || (shouldShowDimmer() && PurchasesVerifier.getInstance().isNotPremium());
    }

    private boolean noDiscreteBrightness() {
        return discreteBrightnessManager.getSet(DISCRETE_BRIGHTNESS_SET).isEmpty();
    }
}
