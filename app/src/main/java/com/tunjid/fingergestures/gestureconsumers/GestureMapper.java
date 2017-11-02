package com.tunjid.fingergestures.gestureconsumers;

import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Pair;

import com.tunjid.fingergestures.App;
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
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.DO_NOTHING;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.INCREASE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.MAXIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.MINIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.NOTIFICATION_UP;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.REDUCE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.TOGGLE_FLASHLIGHT;
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

    private final App app;
    private final int[] actionIds;
    private final String[] actions;

    private final Map<Integer, Integer> gestureActionMap = new HashMap<>();
    private final Map<Integer, Integer> actionGestureMap;

    private final Map<String, Integer> textMap = new HashMap<>();

    private final List<GestureConsumer> consumers = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({UP_GESTURE, DOWN_GESTURE, LEFT_GESTURE, RIGHT_GESTURE})
    public @interface GestureDirection {}

    {
        app = App.getInstance();

        gestureActionMap.put(INCREASE_BRIGHTNESS, R.string.increase_brightness);
        gestureActionMap.put(REDUCE_BRIGHTNESS, R.string.reduce_brightness);
        gestureActionMap.put(MAXIMIZE_BRIGHTNESS, R.string.maximize_brightness);
        gestureActionMap.put(MINIMIZE_BRIGHTNESS, R.string.minimize_brightness);
        gestureActionMap.put(NOTIFICATION_UP, R.string.notification_up);
        gestureActionMap.put(NOTIFICATION_DOWN, R.string.notification_down);
        gestureActionMap.put(TOGGLE_FLASHLIGHT, R.string.toggle_flashlight);
        gestureActionMap.put(DO_NOTHING, R.string.do_nothing);

        textMap.put(UP_GESTURE, R.string.swipe_up);
        textMap.put(DOWN_GESTURE, R.string.swipe_down);
        textMap.put(LEFT_GESTURE, R.string.swipe_left);
        textMap.put(RIGHT_GESTURE, R.string.swipe_right);

        actionGestureMap = invert(gestureActionMap);

        consumers.add(NothingGestureConsumer.getInstance());
        consumers.add(BrightnessGestureConsumer.getInstance());
        consumers.add(NotificationGestureConsumer.getInstance());
        consumers.add(FlashlightGestureConsumer.getInstance());

        Pair<int[], String[]> pair = actionResourceNamePair();
        actionIds = pair.first;
        actions = pair.second;
    }

    public static GestureMapper getInstance() {
        if (instance == null) instance = new GestureMapper();
        return instance;
    }

    private GestureMapper() {}

    public String getGestureName(@GestureDirection String gesture) {
        return app.getString(textMap.get(gesture));
    }

    public String mapGestureToAction(@GestureDirection String direction, int index) {
        int resource = actionIds[index];
        int action = actionGestureMap.get(resource);
        app.getPreferences().edit().putInt(direction, action).apply();
        return app.getString(resource);
    }

    public String getMappedAction(@GestureDirection String gestureDirection) {
        @GestureConsumer.GestureAction
        int action = directionToAction(gestureDirection);
        int stringResource = gestureActionMap.get(action);
        return app.getString(stringResource);
    }

    public CharSequence[] getActions() {
        return actions;
    }

    @Override
    public void onGestureDetectionAvailabilityChanged(boolean available) {
        super.onGestureDetectionAvailabilityChanged(available);
    }

    @Override
    public void onGestureDetected(int raw) {
        super.onGestureDetected(raw);

        @GestureConsumer.GestureAction
        int action = directionToAction(rawToDirection(raw));

        GestureConsumer consumer = consumerForAction(action);
        if (consumer != null) consumer.onGestureActionTriggered(action);
    }

    @Nullable
    private GestureConsumer consumerForAction(@GestureConsumer.GestureAction int action) {
        for (GestureConsumer consumer : consumers)
            if (consumer.accepts(action)) return consumer;
        return null;
    }

    @GestureDirection
    private String rawToDirection(int raw) {
        switch (raw) {
            default:
            case FINGERPRINT_GESTURE_SWIPE_UP:
                return UP_GESTURE;
            case FINGERPRINT_GESTURE_SWIPE_DOWN:
                return DOWN_GESTURE;
            case FINGERPRINT_GESTURE_SWIPE_LEFT:
                return LEFT_GESTURE;
            case FINGERPRINT_GESTURE_SWIPE_RIGHT:
                return RIGHT_GESTURE;
        }
    }

    @GestureConsumer.GestureAction
    private int directionToAction(@GestureDirection String direction) {
        int gesture = app.getPreferences().getInt(direction, UNASSIGNED_GESTURE);

        if (gesture != UNASSIGNED_GESTURE) return gesture;

        // Defaults
        switch (direction) {
            case UP_GESTURE:
                return INCREASE_BRIGHTNESS;
            case DOWN_GESTURE:
                return REDUCE_BRIGHTNESS;
            case LEFT_GESTURE:
                return MINIMIZE_BRIGHTNESS;
            case RIGHT_GESTURE:
                return MAXIMIZE_BRIGHTNESS;
            default:
                return DO_NOTHING;
        }
    }

    private Pair<int[], String[]> actionResourceNamePair() {
        TypedArray typedArray = app.getResources().obtainTypedArray(R.array.action_resources);
        int length = typedArray.length();

        int[] ints = new int[length];
        String[] strings = new String[length];

        for (int i = 0; i < length; i++) {
            ints[i] = typedArray.getResourceId(i, R.string.do_nothing);
            strings[i] = app.getString(ints[i]);
        }

        typedArray.recycle();

        return new Pair<>(ints, strings);
    }

    private static <V, K> Map<V, K> invert(Map<K, V> map) {
        return map.entrySet().stream().collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
