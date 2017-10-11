package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.GestureUtils;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_UP;

public class FingerGestureService extends AccessibilityService {

    private static final String INCREMENT_VALUE = "increment value";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";
    private static final String SLIDER_POSITION = "slider position";

    public static final String BRIGHTNESS_FRACTION = "brightness value";
    private static final String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String QUICK_SETTINGS_DESCRIPTION = "Open quick settings";
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int DEF_POSITION_VALUE = 50;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    BrightnessGestureConsumer.getInstance().onScreenTurnedOff();
                    break;
                case ACTION_NOTIFICATION_DOWN:
                    if (!notificationsShowing()) performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                    else expandQuickSettings(FingerGestureService.this.getRootInActiveWindow());
                    break;
                case ACTION_NOTIFICATION_UP:
                    Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    Application.getContext().sendBroadcast(closeIntent);
                    break;
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        FingerprintGestureController gestureController = getFingerprintGestureController();
        gestureController.registerFingerprintGestureCallback(GestureMapper.getInstance(), new Handler());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ACTION_NOTIFICATION_DOWN);
        filter.addAction(ACTION_NOTIFICATION_UP);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {}

    @Override
    public void onInterrupt() {}

    private void expandQuickSettings(AccessibilityNodeInfo info) {
        int size = info.getChildCount();
        if (size > 0) {
            for (int i = 0; i < size; i++) expandQuickSettings(info.getChild(i));
        }
        else {
            if (!info.getActionList().contains(ACTION_CLICK)) return;

            CharSequence contentDescription = info.getContentDescription();
            if (TextUtils.isEmpty(contentDescription)) return;

            String description = contentDescription.toString();
            if (!description.contains(QUICK_SETTINGS_DESCRIPTION)) return;

            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private boolean notificationsShowing() {
        AccessibilityNodeInfo info = FingerGestureService.this.getRootInActiveWindow();
        return info != null && ANDROID_SYSTEM_UI_PACKAGE.equals(info.getPackageName());
    }

    public static void setBackgroundColor(int color) {
        GestureUtils.getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply();
    }

    public static void setSliderColor(int color) {
        GestureUtils.getPreferences().edit().putInt(SLIDER_COLOR, color).apply();
    }

    public static void setIncrementPercentage(int incrementValue) {
        GestureUtils.getPreferences().edit().putInt(INCREMENT_VALUE, incrementValue).apply();
    }

    public static void setPositionPercentage(int positionPercentage) {
        GestureUtils.getPreferences().edit().putInt(SLIDER_POSITION, positionPercentage).apply();
    }


    public static int getBackgroundColor() {
        return GestureUtils.getPreferences().getInt(BACKGROUND_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorPrimary));
    }

    public static int getSliderColor() {
        return GestureUtils.getPreferences().getInt(SLIDER_COLOR, ContextCompat.getColor(Application.getContext(), R.color.colorAccent));
    }

    public static int getIncrementPercentage() {
        return GestureUtils.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE);
    }

    public static int getPositionPercentage() {
        return GestureUtils.getPreferences().getInt(SLIDER_POSITION, DEF_POSITION_VALUE);
    }
}
