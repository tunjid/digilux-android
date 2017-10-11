package com.tunjid.fingergestures.gestureconsumers;

import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.DO_NOTHING;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.INCREASE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MAXIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.MININIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_UP;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.REDUCE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.getPreferences;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@SuppressLint("UseSparseArrays")
public final class GestureMapper extends FingerprintGestureController.FingerprintGestureCallback {

    private static final int UNASSIGNED_GESTURE = -1;
    public static final String UP_GESTURE = "up gesture";
    public static final String DOWN_GESTURE = "down gesture";
    public static final String LEFT_GESTURE = "left gesture";
    public static final String RIGHT_GESTURE = "right gesture";

    @SuppressLint("StaticFieldLeak")
    private static GestureMapper instance;

    private final Context app;

    private final Map<Integer, String> fingerGestureMap = new HashMap<>();
    private final Map<String, Integer> gestureFingerMap;

    private final Map<Integer, Integer> gestureActionMap = new HashMap<>();
    private final Map<Integer, Integer> actionGestureMap;

    private final Map<String, Integer> textMap = new HashMap<>();

    private final List<Integer> actionResources;
    private final List<String> actions;
    private final List<GestureConsumer> consumers = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({UP_GESTURE, DOWN_GESTURE, LEFT_GESTURE, RIGHT_GESTURE})
    public @interface Gesture {}

    {
        app = Application.getContext();

        fingerGestureMap.put(FINGERPRINT_GESTURE_SWIPE_UP, UP_GESTURE);
        fingerGestureMap.put(FINGERPRINT_GESTURE_SWIPE_DOWN, DOWN_GESTURE);
        fingerGestureMap.put(FINGERPRINT_GESTURE_SWIPE_LEFT, LEFT_GESTURE);
        fingerGestureMap.put(FINGERPRINT_GESTURE_SWIPE_RIGHT, RIGHT_GESTURE);

        gestureActionMap.put(INCREASE_BRIGHTNESS, R.string.increase_brightness);
        gestureActionMap.put(REDUCE_BRIGHTNESS, R.string.reduce_brightness);
        gestureActionMap.put(MAXIMIZE_BRIGHTNESS, R.string.maximize_brightness);
        gestureActionMap.put(MININIMIZE_BRIGHTNESS, R.string.minimize_brightness);
        gestureActionMap.put(NOTIFICATION_UP, R.string.notification_up);
        gestureActionMap.put(NOTIFICATION_DOWN, R.string.notification_down);
        gestureActionMap.put(DO_NOTHING, R.string.do_nothing);
//        gestureActionMap.put(NIGHT_MODE_ON, R.string.night_mode_on);
//        gestureActionMap.put(NIGHT_MODE_OFF, R.string.night_mode_off);

        textMap.put(UP_GESTURE, R.string.swipe_up);
        textMap.put(DOWN_GESTURE, R.string.swipe_down);
        textMap.put(LEFT_GESTURE, R.string.swipe_left);
        textMap.put(RIGHT_GESTURE, R.string.swipe_right);

        gestureFingerMap = invert(fingerGestureMap);
        actionGestureMap = invert(gestureActionMap);

        actionResources = gestureActionMap.values().stream().sorted(comparing(actionGestureMap::get)).collect(toList());
        actions = actionResources.stream().map(app::getString).collect(toList());

        consumers.add(NothingGestureConsumer.getInstance());
        consumers.add(BrightnessGestureConsumer.getInstance());
//        consumers.add(NightLightGestureConsumer.getInstance());
        consumers.add(NotificationGestureConsumer.getInstance());
    }

    public static GestureMapper getInstance() {
        if (instance == null) instance = new GestureMapper();
        return instance;
    }

    private GestureMapper() {}

    public String getGestureName(@Gesture String gesture) {
        return app.getString(textMap.get(gesture));
    }

    public String mapGestureToAction(@Gesture String gesture, int index) {
        int resource = actionResources.get(index);
        int action = actionGestureMap.get(resource);
        getPreferences().edit().putInt(gesture, action).apply();
        return app.getString(resource);
    }

    public String getMappedAction(@Gesture String gesture) {
        int raw = gestureFingerMap.get(gesture);
        int action = fingerPrintToGesture(raw);
        int stringResource = gestureActionMap.get(action);
        return app.getString(stringResource);
    }

    public CharSequence[] getActions() {
        return actions.toArray(new CharSequence[actions.size()]);
    }

    @Override
    public void onGestureDetectionAvailabilityChanged(boolean available) {
        super.onGestureDetectionAvailabilityChanged(available);
    }

    @Override
    public void onGestureDetected(int raw) {
        super.onGestureDetected(raw);

        @GestureUtils.GestureAction int gesture = fingerPrintToGesture(raw);
        GestureConsumer consumer = consumerForGesture(gesture);
        if (consumer != null) consumer.onGestureActionTriggered(gesture);
    }

    @Nullable
    private GestureConsumer consumerForGesture(@GestureUtils.GestureAction int gesture) {
        for (GestureConsumer consumer : consumers)
            if (consumer.accepts(gesture)) return consumer;
        return null;
    }

    @GestureUtils.GestureAction
    private int fingerPrintToGesture(int raw) {
        String gestureKey = fingerGestureMap.get(raw);
        int gesture = getPreferences().getInt(gestureKey, UNASSIGNED_GESTURE);

        if (gesture == UNASSIGNED_GESTURE) {
            switch (gestureKey) {
                case UP_GESTURE:
                    return INCREASE_BRIGHTNESS;
                case DOWN_GESTURE:
                    return REDUCE_BRIGHTNESS;
                case LEFT_GESTURE:
                    return MININIMIZE_BRIGHTNESS;
                case RIGHT_GESTURE:
                    return MAXIMIZE_BRIGHTNESS;
                default:
                    return DO_NOTHING;
            }
        }
        else return gesture;
    }

    private static <V, K> Map<V, K> invert(Map<K, V> map) {
        return map.entrySet().stream().collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
