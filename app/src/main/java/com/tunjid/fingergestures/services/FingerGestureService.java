package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_UP;

public class FingerGestureService extends AccessibilityService {

    private static final String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String QUICK_SETTINGS_DESCRIPTION = "Open quick settings";

    @Nullable
    private View overlayView;

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
                    App.getInstance().sendBroadcast(closeIntent);
                    break;
                case BrightnessGestureConsumer.ACTION_SCREEN_DIMMER_CHANGED:
                    adjustDimmer();
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
        filter.addAction(BrightnessGestureConsumer.ACTION_SCREEN_DIMMER_CHANGED);

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

    private void adjustDimmer() {
        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        float dimAmount = brightnessGestureConsumer.getScreenDimmerDimPercent();
        WindowManager windowManager = getSystemService(WindowManager.class);

        if (brightnessGestureConsumer.shouldShowDimmer()) {
            WindowManager.LayoutParams params;
            if (overlayView == null) params = getLayoutParams(windowManager);
            else params = ((WindowManager.LayoutParams) overlayView.getLayoutParams());

            params.alpha = 0.1F;
            params.dimAmount = dimAmount;
            windowManager.updateViewLayout(overlayView, params);
        }
        else if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @NonNull
    @SuppressLint("InflateParams")
    private WindowManager.LayoutParams getLayoutParams(WindowManager windowManager) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.window_overlay, null);
        windowManager.addView(overlayView, params);

        return params;
    }
}
