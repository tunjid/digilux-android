package com.tunjid.fingergestures.gestureconsumers;

import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Pair;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

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
import static io.reactivex.Flowable.timer;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

@SuppressLint("UseSparseArrays")
public final class GestureMapper extends FingerprintGestureController.FingerprintGestureCallback {

    private static final int UNASSIGNED_GESTURE = -1;
    private static final int ONGOING_RESET_DELAY = 2;
    private static final int MAX_DOUBLE_SWIPE_DELAY = 800;
    private static final int DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50;

    public static final String UP_GESTURE = "up gesture";
    public static final String DOWN_GESTURE = "down gesture";
    public static final String LEFT_GESTURE = "left gesture";
    public static final String RIGHT_GESTURE = "right gesture";
    private static final String DOUBLE_UP_GESTURE = "double up gesture";
    private static final String DOUBLE_DOWN_GESTURE = "double down gesture";
    private static final String DOUBLE_LEFT_GESTURE = "double left gesture";
    private static final String DOUBLE_RIGHT_GESTURE = "double right gesture";
    private static final String DOUBLE_SWIPE_DELAY = "double swipe delay";

    @SuppressLint("StaticFieldLeak")
    private static GestureMapper instance;

    private final App app;
    private final int[] actionIds;
    private final String[] actions;

    private final Map<String, Integer> textMap;
    private final Map<Integer, Integer> gestureActionMap;
    private final Map<Integer, Integer> actionGestureMap;

    private final List<GestureConsumer> consumers;
    private final AtomicReference<String> directionReference;

    private Disposable isSwipingDisposable;
    private Disposable doubleSwipeDisposable;
    private boolean isOngoing;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({UP_GESTURE, DOWN_GESTURE, LEFT_GESTURE, RIGHT_GESTURE,
            DOUBLE_UP_GESTURE, DOUBLE_DOWN_GESTURE, DOUBLE_LEFT_GESTURE, DOUBLE_RIGHT_GESTURE})
    public @interface GestureDirection {}

    {
        app = App.getInstance();

        textMap = new HashMap<>();
        gestureActionMap = new HashMap<>();
        consumers = new ArrayList<>();
        directionReference = new AtomicReference<>();

        textMap.put(UP_GESTURE, R.string.swipe_up);
        textMap.put(DOWN_GESTURE, R.string.swipe_down);
        textMap.put(LEFT_GESTURE, R.string.swipe_left);
        textMap.put(RIGHT_GESTURE, R.string.swipe_right);
        textMap.put(DOUBLE_UP_GESTURE, R.string.double_swipe_up);
        textMap.put(DOUBLE_DOWN_GESTURE, R.string.double_swipe_down);
        textMap.put(DOUBLE_LEFT_GESTURE, R.string.double_swipe_left);
        textMap.put(DOUBLE_RIGHT_GESTURE, R.string.double_swipe_right);

        gestureActionMap.put(INCREASE_BRIGHTNESS, R.string.increase_brightness);
        gestureActionMap.put(REDUCE_BRIGHTNESS, R.string.reduce_brightness);
        gestureActionMap.put(MAXIMIZE_BRIGHTNESS, R.string.maximize_brightness);
        gestureActionMap.put(MINIMIZE_BRIGHTNESS, R.string.minimize_brightness);
        gestureActionMap.put(NOTIFICATION_UP, R.string.notification_up);
        gestureActionMap.put(NOTIFICATION_DOWN, R.string.notification_down);
        gestureActionMap.put(TOGGLE_FLASHLIGHT, R.string.toggle_flashlight);
        gestureActionMap.put(DO_NOTHING, R.string.do_nothing);

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

    public String getDirectionName(@GestureDirection String direction) {
        return app.getString(textMap.get(direction));
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

    @GestureDirection
    public String doubleDirection(@GestureDirection String direction) {
        return match(direction, direction);
    }

    public CharSequence[] getActions() {
        return actions;
    }

    public int getDoubleSwipeDelay() {
        return app.getPreferences().getInt(DOUBLE_SWIPE_DELAY, DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE);
    }

    public void setDoubleSwipeDelay(int percentage) {
        app.getPreferences().edit().putInt(DOUBLE_SWIPE_DELAY, percentage).apply();
    }

    public String getSwipeDelayText(int percentage) {
        return app.getString(PurchasesManager.getInstance().isNotPremium()
                ? R.string.go_premium_text
                : R.string.double_swipe_delay, delayPercentageToMillis(percentage));
    }

    @Override
    public void onGestureDetectionAvailabilityChanged(boolean available) {
        super.onGestureDetectionAvailabilityChanged(available);
    }

    @Override
    public void onGestureDetected(int raw) {
        super.onGestureDetected(raw);

        String newDirection = rawToDirection(raw);
        String originalDirection = directionReference.get();

        boolean hasPreviousSwipe = originalDirection != null;
        boolean hasPendingAction = doubleSwipeDisposable != null && !doubleSwipeDisposable.isDisposed();
        boolean hasNoDoubleSwipe = directionToAction(doubleDirection(newDirection)) == DO_NOTHING;

        // Keep responsiveness if user has not mapped gesture to a double swipe
        if (!hasPreviousSwipe && hasNoDoubleSwipe) {
            performAction(newDirection);
            return;
        }

        // User has been swiping repeatedly in a certain direction
        if (isOngoing) {
            if (isSwipingDisposable != null) isSwipingDisposable.dispose();
            resetIsOngoing();
            performAction(newDirection);
            return;
        }

        // Is canceling an existing double gesture to continue a single gesture
        if (hasPreviousSwipe && hasPendingAction && isDouble(originalDirection)) {
            doubleSwipeDisposable.dispose();
            directionReference.set(null);
            isOngoing = true;
            resetIsOngoing();
            performAction(newDirection);
            return;
        }

        // Never completed a double gesture
        if (hasPreviousSwipe && !originalDirection.equals(newDirection)) {
            if (hasPendingAction) doubleSwipeDisposable.dispose();
            directionReference.set(null);
            performAction(newDirection);
            return;
        }

        directionReference.set(match(originalDirection, newDirection));

        if (hasPendingAction) doubleSwipeDisposable.dispose();

        doubleSwipeDisposable = timer(delayPercentageToMillis(getDoubleSwipeDelay()), MILLISECONDS)
                .subscribe(ignored -> {
                    String direction = directionReference.getAndSet(null);
                    if (direction == null) return;
                    performAction(direction);
                }, this::onError);
    }

    private void performAction(@GestureDirection String direction) {
        @GestureConsumer.GestureAction
        int action = directionToAction(direction);
        GestureConsumer consumer = consumerForAction(action);
        if (consumer != null) consumer.onGestureActionTriggered(action);
    }

    @Nullable
    private GestureConsumer consumerForAction(@GestureConsumer.GestureAction int action) {
        for (GestureConsumer consumer : consumers) if (consumer.accepts(action)) return consumer;
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
        if (isDouble(direction) && PurchasesManager.getInstance().isNotPremium()) return DO_NOTHING;

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

    private String match(@GestureDirection String original, @GestureDirection String updated) {
        if (!updated.equals(original)) return updated;

        switch (updated) {
            case UP_GESTURE:
                return DOUBLE_UP_GESTURE;
            case DOWN_GESTURE:
                return DOUBLE_DOWN_GESTURE;
            case LEFT_GESTURE:
                return DOUBLE_LEFT_GESTURE;
            case RIGHT_GESTURE:
                return DOUBLE_RIGHT_GESTURE;
            default:
                return updated;
        }
    }

    private boolean isDouble(String direction) {
        switch (direction) {
            case DOUBLE_UP_GESTURE:
            case DOUBLE_DOWN_GESTURE:
            case DOUBLE_LEFT_GESTURE:
            case DOUBLE_RIGHT_GESTURE:
                return true;
            default:
                return false;
        }
    }

    private int delayPercentageToMillis(int percentage) {
        return (int) (percentage * MAX_DOUBLE_SWIPE_DELAY / 100F);
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

    private void resetIsOngoing() {
        isSwipingDisposable = timer(ONGOING_RESET_DELAY, SECONDS).subscribe(i -> isOngoing = false, this::onError);
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private static <V, K> Map<V, K> invert(Map<K, V> map) {
        return map.entrySet().stream().collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
