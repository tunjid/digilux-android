package com.tunjid.fingergestures.gestureconsumers;

import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.StringRes;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import io.reactivex.disposables.Disposable;

import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT;
import static android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.GLOBAL_BACK;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.DO_NOTHING;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.GLOBAL_POWER_DIALOG;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.GLOBAL_SPLIT_SCREEN;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.INCREASE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.GLOBAL_HOME;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.MAXIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.MINIMIZE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.GLOBAL_RECENTS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.NOTIFICATION_TOGGLE;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.NOTIFICATION_UP;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.REDUCE_BRIGHTNESS;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.TOGGLE_AUTO_ROTATE;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.TOGGLE_DOCK;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.TOGGLE_FLASHLIGHT;
import static io.reactivex.Flowable.timer;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressLint("UseSparseArrays")
public final class GestureMapper extends FingerprintGestureController.FingerprintGestureCallback {

    private static final int UNASSIGNED_GESTURE = -1;
    private static final int ONGOING_RESET_DELAY = 1;
    private static final int MAX_DOUBLE_SWIPE_DELAY = 1000;
    private static final int DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50;

    public static final String UP_GESTURE = "up gesture";
    public static final String DOWN_GESTURE = "down gesture";
    public static final String LEFT_GESTURE = "left gesture";
    public static final String RIGHT_GESTURE = "right gesture";
    public static final String DOUBLE_UP_GESTURE = "double up gesture";
    public static final String DOUBLE_DOWN_GESTURE = "double down gesture";
    public static final String DOUBLE_LEFT_GESTURE = "double left gesture";
    public static final String DOUBLE_RIGHT_GESTURE = "double right gesture";
    private static final String DOUBLE_SWIPE_DELAY = "double swipe delay";

    @SuppressLint("StaticFieldLeak")
    private static GestureMapper instance;

    private final App app;

    private final int[] actionIds;
    private final GestureConsumer[] consumers;

    private final AtomicReference<String> directionReference;

    private Disposable isSwipingDisposable;
    private Disposable doubleSwipeDisposable;
    private boolean isOngoing;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({UP_GESTURE, DOWN_GESTURE, LEFT_GESTURE, RIGHT_GESTURE,
            DOUBLE_UP_GESTURE, DOUBLE_DOWN_GESTURE, DOUBLE_LEFT_GESTURE, DOUBLE_RIGHT_GESTURE})
    public @interface GestureDirection {}

    private GestureMapper() {
        app = App.getInstance();
        directionReference = new AtomicReference<>();

        consumers = new GestureConsumer[]{
                NothingGestureConsumer.getInstance(),
                BrightnessGestureConsumer.getInstance(),
                NotificationGestureConsumer.getInstance(),
                FlashlightGestureConsumer.getInstance(),
                DockingGestureConsumer.getInstance(),
                RotationGestureConsumer.getInstance(),
                GlobalActionGestureConsumer.getInstance()};

        actionIds = getActionIds();
    }

    public static GestureMapper getInstance() {
        if (instance == null) instance = new GestureMapper();
        return instance;
    }

    public void mapGestureToAction(@GestureDirection String direction, @GestureConsumer.GestureAction int action) {
        app.getPreferences().edit().putInt(direction, action).apply();
    }

    public String getMappedAction(@GestureDirection String gestureDirection) {
        @GestureConsumer.GestureAction
        int action = directionToAction(gestureDirection);
        int stringResource = resourceForAction(action);
        return app.getString(stringResource);
    }

    @GestureDirection
    public String doubleDirection(@GestureDirection String direction) {
        return match(direction, direction);
    }

    public int[] getActions() {
        return IntStream.of(actionIds).map(this::actionForResource).toArray();
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

    public void performAction(@GestureConsumer.GestureAction int action) {
        GestureConsumer consumer = consumerForAction(action);
        if (consumer != null) consumer.onGestureActionTriggered(action);
    }

    private void performAction(@GestureDirection String direction) {
        @GestureConsumer.GestureAction
        int action = directionToAction(direction);
        performAction(action);
    }

    @Nullable
    private GestureConsumer consumerForAction(@GestureConsumer.GestureAction int action) {
        for (GestureConsumer consumer : consumers) if (consumer.accepts(action)) return consumer;
        return null;
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private void resetIsOngoing() {
        isSwipingDisposable = App.delay(ONGOING_RESET_DELAY, SECONDS, () -> isOngoing = false);
    }

    private int delayPercentageToMillis(int percentage) {
        return (int) (percentage * MAX_DOUBLE_SWIPE_DELAY / 100F);
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
                return NOTIFICATION_UP;
            case DOWN_GESTURE:
                return NOTIFICATION_DOWN;
            case LEFT_GESTURE:
                return REDUCE_BRIGHTNESS;
            case RIGHT_GESTURE:
                return INCREASE_BRIGHTNESS;
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

    @GestureConsumer.GestureAction
    private int actionForResource(int resource) {
        switch (resource) {
            default:
            case R.string.do_nothing:
                return DO_NOTHING;
            case R.string.increase_brightness:
                return INCREASE_BRIGHTNESS;
            case R.string.reduce_brightness:
                return REDUCE_BRIGHTNESS;
            case R.string.maximize_brightness:
                return MAXIMIZE_BRIGHTNESS;
            case R.string.minimize_brightness:
                return MINIMIZE_BRIGHTNESS;
            case R.string.notification_up:
                return NOTIFICATION_UP;
            case R.string.notification_down:
                return NOTIFICATION_DOWN;
            case R.string.toggle_notifications:
                return NOTIFICATION_TOGGLE;
            case R.string.toggle_flashlight:
                return TOGGLE_FLASHLIGHT;
            case R.string.toggle_dock:
                return TOGGLE_DOCK;
            case R.string.toggle_auto_rotate:
                return TOGGLE_AUTO_ROTATE;
            case R.string.global_home:
                return GLOBAL_HOME;
            case R.string.global_back:
                return GLOBAL_BACK;
            case R.string.global_recents:
                return GLOBAL_RECENTS;
            case R.string.global_split_screen:
                return GLOBAL_SPLIT_SCREEN;
            case R.string.global_power_dialog:
                return GLOBAL_POWER_DIALOG;
        }
    }

    @StringRes
    public int resourceForAction(@GestureConsumer.GestureAction int action) {
        switch (action) {
            default:
            case DO_NOTHING:
                return R.string.do_nothing;
            case INCREASE_BRIGHTNESS:
                return R.string.increase_brightness;
            case REDUCE_BRIGHTNESS:
                return R.string.reduce_brightness;
            case MAXIMIZE_BRIGHTNESS:
                return R.string.maximize_brightness;
            case MINIMIZE_BRIGHTNESS:
                return R.string.minimize_brightness;
            case NOTIFICATION_UP:
                return R.string.notification_up;
            case NOTIFICATION_DOWN:
                return R.string.notification_down;
            case NOTIFICATION_TOGGLE:
                return R.string.toggle_notifications;
            case TOGGLE_FLASHLIGHT:
                return R.string.toggle_flashlight;
            case TOGGLE_DOCK:
                return R.string.toggle_dock;
            case TOGGLE_AUTO_ROTATE:
                return R.string.toggle_auto_rotate;
            case GLOBAL_HOME:
                return R.string.global_home;
            case GLOBAL_BACK:
                return R.string.global_back;
            case GLOBAL_RECENTS:
                return R.string.global_recents;
            case GLOBAL_SPLIT_SCREEN:
                return R.string.global_split_screen;
            case GLOBAL_POWER_DIALOG:
                return R.string.global_power_dialog;
        }
    }

    public String getDirectionName(@GestureDirection String direction) {
        switch (direction) {
            case UP_GESTURE:
                return app.getString(R.string.swipe_up);
            case DOWN_GESTURE:
                return app.getString(R.string.swipe_down);
            case LEFT_GESTURE:
                return app.getString(R.string.swipe_left);
            case RIGHT_GESTURE:
                return app.getString(R.string.swipe_right);
            case DOUBLE_UP_GESTURE:
                return app.getString(R.string.double_swipe_up);
            case DOUBLE_DOWN_GESTURE:
                return app.getString(R.string.double_swipe_down);
            case DOUBLE_LEFT_GESTURE:
                return app.getString(R.string.double_swipe_left);
            case DOUBLE_RIGHT_GESTURE:
                return app.getString(R.string.double_swipe_right);
            default:
                return "";
        }
    }

    private int[] getActionIds() {
        TypedArray typedArray = app.getResources().obtainTypedArray(R.array.action_resources);
        int length = typedArray.length();

        int[] ints = new int[length];
        for (int i = 0; i < length; i++) ints[i] = typedArray.getResourceId(i, R.string.do_nothing);

        typedArray.recycle();

        return ints;
    }
}
