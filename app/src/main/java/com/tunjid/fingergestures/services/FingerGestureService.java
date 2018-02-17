package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.FingerprintGestureController;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PixelFormat;
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
import com.tunjid.fingergestures.PopUpManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.PopupActivity;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import static android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
import static com.tunjid.fingergestures.PopUpManager.ACTION_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.PopUpManager.EXTRA_SHOWS_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.ACTION_SCREEN_DIMMER_CHANGED;
import static com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.ACTION_TOGGLE_DOCK;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_UP;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ACTION_WATCH_WINDOW_CHANGES;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.EXTRA_WATCHES_WINDOWS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class FingerGestureService extends AccessibilityService {

    public static final String ACTION_SHOW_SNACK_BAR = "action show snack bar";
    public static final String EXTRA_SHOW_SNACK_BAR = " extrashow snack bar";
    public static final String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String RESOURCE_OPEN_SETTINGS = "accessibility_quick_settings_settings";
    private static final String RESOURCE_EXPAND_QUICK_SETTINGS = "accessibility_quick_settings_expand";
    private static final String DEFAULT_OPEN_SETTINGS = "Open settings";
    private static final String DEFAULT_EXPAND_QUICK_SETTINGS = "Open quick settings";
    private static final String STRING_RESOURCE = "string";
    private static final int INVALID_RESOURCE = 0;
    private static final int DELAY = 50;

    @Nullable
    private View overlayView;
    private final AccessibilityButtonController.AccessibilityButtonCallback accessibilityButtonCallback = new AccessibilityButtonController.AccessibilityButtonCallback() {
        @Override
        public void onClicked(AccessibilityButtonController controller) {
            Intent intent = new Intent(FingerGestureService.this, PopupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_SCREEN_ON:
                    BrightnessGestureConsumer.getInstance().onScreenTurnedOn();
                    break;
                case ACTION_NOTIFICATION_DOWN:
                    if (notificationsShowing()) expandQuickSettings();
                    else performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                    break;
                case ACTION_NOTIFICATION_UP:
                    Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    closeIntent.setPackage(ANDROID_SYSTEM_UI_PACKAGE);
                    App.getInstance().sendBroadcast(closeIntent);
                    break;
                case ACTION_SCREEN_DIMMER_CHANGED:
                    adjustDimmer();
                    break;
                case ACTION_TOGGLE_DOCK:
                    performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                    App.delay(DELAY, MILLISECONDS, () -> performGlobalAction(GLOBAL_ACTION_RECENTS));
                    break;
                case ACTION_WATCH_WINDOW_CHANGES:
                    setWatchesWindows(intent.getBooleanExtra(EXTRA_WATCHES_WINDOWS, false));
                    break;
                case ACTION_ACCESSIBILITY_BUTTON:
                    setShowsAccessibilityButton(intent.getBooleanExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, false));
                    break;
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        FingerprintGestureController gestureController = getFingerprintGestureController();
        gestureController.registerFingerprintGestureCallback(GestureMapper.getInstance(), null);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCREEN_DIMMER_CHANGED);
        filter.addAction(ACTION_ACCESSIBILITY_BUTTON);
        filter.addAction(ACTION_WATCH_WINDOW_CHANGES);
        filter.addAction(ACTION_NOTIFICATION_DOWN);
        filter.addAction(ACTION_NOTIFICATION_UP);
        filter.addAction(ACTION_TOGGLE_DOCK);
        filter.addAction(ACTION_SCREEN_ON);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        registerReceiver(receiver, filter);
        setWatchesWindows(RotationGestureConsumer.getInstance().canAutoRotate());
        setShowsAccessibilityButton(PopUpManager.getInstance().hasAccessibilityButton());
    }

    @Override
    public void onDestroy() {
        getAccessibilityButtonController().unregisterAccessibilityButtonCallback(accessibilityButtonCallback);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        RotationGestureConsumer.getInstance().onAccessibilityEvent(event);
    }

    @Override
    public void onInterrupt() {}

    private void expandQuickSettings() {
        AccessibilityNodeInfo info = findNode(getRootInActiveWindow(), getSystemUiString(RESOURCE_EXPAND_QUICK_SETTINGS, DEFAULT_EXPAND_QUICK_SETTINGS));
        if (info != null) info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean notificationsShowing() {
        AccessibilityNodeInfo info = FingerGestureService.this.getRootInActiveWindow();
        return info != null && ANDROID_SYSTEM_UI_PACKAGE.equals(info.getPackageName()) && isSettingsCogVisible();
    }

    private boolean isSettingsCogVisible() {
        return findNode(getRootInActiveWindow(), getSystemUiString(RESOURCE_OPEN_SETTINGS, DEFAULT_OPEN_SETTINGS)) != null;
    }

    private void adjustDimmer() {
        WindowManager windowManager = getSystemService(WindowManager.class);
        if (windowManager == null) return;

        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        float dimAmount = brightnessGestureConsumer.getScreenDimmerDimPercent();

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

        LayoutInflater inflater = getSystemService(LayoutInflater.class);
        if (inflater == null) return params;

        overlayView = inflater.inflate(R.layout.window_overlay, null);
        windowManager.addView(overlayView, params);

        return params;
    }

    private void setWatchesWindows(boolean enabled) {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = enabled ? TYPE_WINDOW_STATE_CHANGED : 0;
        setServiceInfo(info);
    }

    private void setShowsAccessibilityButton(boolean enabled) {
        AccessibilityButtonController controller = getAccessibilityButtonController();
        //if (!controller.isAccessibilityButtonAvailable()) return;

        AccessibilityServiceInfo info = getServiceInfo();

        if (enabled) info.flags |= FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        else info.flags = info.flags & ~FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        setServiceInfo(info);
        controller.registerAccessibilityButtonCallback(accessibilityButtonCallback);
    }

    private String getSystemUiString(String resourceID, String defaultValue) {
        Resources resources = getSystemUiResources();
        if (resources == null) return defaultValue;

        int id = resources.getIdentifier(resourceID, STRING_RESOURCE, ANDROID_SYSTEM_UI_PACKAGE);

        if (id == INVALID_RESOURCE) return defaultValue;
        else return resources.getString(id);
    }

    @Nullable
    private Resources getSystemUiResources() {
        PackageManager packageManager = getPackageManager();

        try {return packageManager.getResourcesForApplication(ANDROID_SYSTEM_UI_PACKAGE);}
        catch (Exception e) {return null;}
    }

    @Nullable
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo info, String name) {
        if (info == null) return null;
        int size = info.getChildCount();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                AccessibilityNodeInfo node = findNode(info.getChild(i), name);
                if (node != null) return node;
            }
        }
        else {
            if (!info.getActionList().contains(ACTION_CLICK)) return null;

            CharSequence contentDescription = info.getContentDescription();
            if (TextUtils.isEmpty(contentDescription)) return null;

            String description = contentDescription.toString();
            if (description.contains(name)) return info;
        }
        return null;
    }
}
