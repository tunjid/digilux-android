/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.services;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;

import static android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
import static com.tunjid.fingergestures.App.withApp;
import static com.tunjid.fingergestures.PopUpGestureConsumer.ACTION_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.PopUpGestureConsumer.ACTION_SHOW_POPUP;
import static com.tunjid.fingergestures.PopUpGestureConsumer.EXTRA_SHOWS_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer.ACTION_EXPAND_VOLUME_CONTROLS;
import static com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.ACTION_SCREEN_DIMMER_CHANGED;
import static com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.ACTION_TOGGLE_DOCK;
import static com.tunjid.fingergestures.gestureconsumers.GlobalActionGestureConsumer.ACTION_GLOBAL_ACTION;
import static com.tunjid.fingergestures.gestureconsumers.GlobalActionGestureConsumer.EXTRA_GLOBAL_ACTION;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_TOGGLE;
import static com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.ACTION_NOTIFICATION_UP;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ACTION_WATCH_WINDOW_CHANGES;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.EXTRA_WATCHES_WINDOWS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class FingerGestureService extends AccessibilityService {

    public static final String ACTION_SHOW_SNACK_BAR = "action show snack bar";
    public static final String EXTRA_SHOW_SNACK_BAR = " extra show snack bar";
    public static final String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String RESOURCE_EXPAND_VOLUME_CONTROLS = "accessibility_volume_expand";
    private static final String RESOURCE_EXPAND_QUICK_SETTINGS = "accessibility_quick_settings_expand";
    private static final String RESOURCE_EDIT_QUICK_SETTINGS = "accessibility_quick_settings_edit";
    private static final String DEFAULT_EXPAND_VOLUME = "Expand";
    private static final String DEFAULT_EXPAND_QUICK_SETTINGS = "Open quick settings";
    private static final String DEFAULT_EDIT_QUICK_SETTINGS = "Edit order of settings";

    private static final String STRING_RESOURCE = "string";
    private static final int INVALID_RESOURCE = 0;
    private static final int DELAY = 50;

    @Nullable
    private View overlayView;

    private HandlerThread gestureThread;
    private Disposable broadcastDisposable;

    private final AccessibilityButtonCallback accessibilityButtonCallback = new AccessibilityButtonCallback() {
        @Override
        public void onClicked(AccessibilityButtonController controller) { PopUpGestureConsumer.Companion.getInstance().showPopup(); }
    };

    private final BroadcastReceiver screenWakeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { onIntentReceived(intent); }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if (gestureThread != null) gestureThread.quitSafely();
        gestureThread = new HandlerThread("GestureThread");
        gestureThread.start();

        getFingerprintGestureController().registerFingerprintGestureCallback(GestureMapper.Companion.getInstance(), new Handler(gestureThread.getLooper()));

        BackgroundManager.Companion.getInstance().restoreWallpaperChange();

        subscribeToBroadcasts();
        registerReceiver(screenWakeReceiver, new IntentFilter(ACTION_SCREEN_ON));
        setWatchesWindows(RotationGestureConsumer.Companion.getInstance().canAutoRotate());
        setShowsAccessibilityButton(PopUpGestureConsumer.Companion.getInstance().hasAccessibilityButton());
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenWakeReceiver);
        getFingerprintGestureController().unregisterFingerprintGestureCallback(GestureMapper.Companion.getInstance());
        getAccessibilityButtonController().unregisterAccessibilityButtonCallback(accessibilityButtonCallback);

        if (broadcastDisposable != null) broadcastDisposable.dispose();
        if (gestureThread != null) gestureThread.quitSafely();
        gestureThread = null;

        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        RotationGestureConsumer.Companion.getInstance().onAccessibilityEvent(event);
    }

    @Override
    public void onInterrupt() {}

    private void expandQuickSettings() {
        AccessibilityNodeInfo info = findNode(getRootInActiveWindow(), getSystemUiString(RESOURCE_EXPAND_QUICK_SETTINGS, DEFAULT_EXPAND_QUICK_SETTINGS));
        if (info != null) info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        else swipeDown();
    }

    private boolean notificationsShowing() {
        AccessibilityNodeInfo info = FingerGestureService.this.getRootInActiveWindow();
        return info != null && info.getPackageName() != null && ANDROID_SYSTEM_UI_PACKAGE.equals(info.getPackageName().toString());
    }

    private boolean isQuickSettingsShowing() {
        return findNode(getRootInActiveWindow(), getSystemUiString(RESOURCE_EDIT_QUICK_SETTINGS, DEFAULT_EDIT_QUICK_SETTINGS)) != null;
    }

    private void expandAudioControls() {
        getWindows().stream()
                .map(AccessibilityWindowInfo::getRoot)
                .filter(Objects::nonNull)
                .map(nodeInfo -> findNode(nodeInfo, getSystemUiString(RESOURCE_EXPAND_VOLUME_CONTROLS, DEFAULT_EXPAND_VOLUME)))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(accessibilityNodeInfo -> accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK));
    }

    private void closeNotifications() {
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        closeIntent.setPackage(ANDROID_SYSTEM_UI_PACKAGE);
        sendBroadcast(closeIntent);
    }

    private void swipeDown() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path path = new Path();

        path.moveTo(0, 0);
        path.lineTo(displayMetrics.widthPixels, displayMetrics.heightPixels);
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 20));

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {}, null);
    }

    private void adjustDimmer() {
        WindowManager windowManager = getSystemService(WindowManager.class);
        if (windowManager == null) return;

        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.Companion.getInstance();
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
        AccessibilityServiceInfo info = getServiceInfo();

        if (enabled) info.flags |= FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        else info.flags = info.flags & ~FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        setServiceInfo(info);
        controller.registerAccessibilityButtonCallback(accessibilityButtonCallback);
    }

    private void subscribeToBroadcasts() {
        Companion.withApp(app -> broadcastDisposable = app.broadcasts()
                .filter(this::intentMatches)
                .subscribe(this::onIntentReceived, error -> {
                    error.printStackTrace();
                    subscribeToBroadcasts(); // Resubscribe on error
                }));
    }

    private void onIntentReceived(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ACTION_SCREEN_ON:
                BrightnessGestureConsumer.Companion.getInstance().onScreenTurnedOn();
                break;
            case Companion.getACTION_NOTIFICATION_DOWN():
                if (notificationsShowing()) expandQuickSettings();
                else performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                break;
            case Companion.getACTION_NOTIFICATION_UP():
                closeNotifications();
                break;
            case Companion.getACTION_NOTIFICATION_TOGGLE():
                if (isQuickSettingsShowing()) closeNotifications();
                else if (notificationsShowing()) expandQuickSettings();
                else performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                break;
            case Companion.getACTION_EXPAND_VOLUME_CONTROLS():
                expandAudioControls();
                break;
            case Companion.getACTION_SCREEN_DIMMER_CHANGED():
                adjustDimmer();
                break;
            case Companion.getACTION_TOGGLE_DOCK():
                performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                App.Companion.delay(DELAY, MILLISECONDS, () -> performGlobalAction(GLOBAL_ACTION_RECENTS));
                break;
            case Companion.getACTION_WATCH_WINDOW_CHANGES():
                setWatchesWindows(intent.getBooleanExtra(Companion.getEXTRA_WATCHES_WINDOWS(), false));
                break;
            case Companion.getACTION_ACCESSIBILITY_BUTTON():
                setShowsAccessibilityButton(intent.getBooleanExtra(Companion.getEXTRA_SHOWS_ACCESSIBILITY_BUTTON(), false));
                break;
            case Companion.getACTION_GLOBAL_ACTION():
                int globalAction = intent.getIntExtra(Companion.getEXTRA_GLOBAL_ACTION(), -1);
                if (globalAction != -1) performGlobalAction(globalAction);
                break;
            case Companion.getACTION_SHOW_POPUP():
                PopUpGestureConsumer.Companion.getInstance().showPopup();
                break;
        }
    }

    private boolean intentMatches(Intent intent) {
        String action = intent.getAction();
        if (action == null) return false;

        switch (action) {
            case Companion.getACTION_SCREEN_DIMMER_CHANGED():
            case Companion.getACTION_EXPAND_VOLUME_CONTROLS():
            case Companion.getACTION_ACCESSIBILITY_BUTTON():
            case Companion.getACTION_WATCH_WINDOW_CHANGES():
            case Companion.getACTION_NOTIFICATION_TOGGLE():
            case Companion.getACTION_NOTIFICATION_DOWN():
            case Companion.getACTION_NOTIFICATION_UP():
            case Companion.getACTION_GLOBAL_ACTION():
            case Companion.getACTION_TOGGLE_DOCK():
            case Companion.getACTION_SHOW_POPUP():
            case ACTION_SCREEN_ON:
                return true;
            default:
                return false;
        }
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

        if (size > 0) for (int i = 0; i < size; i++) {
            AccessibilityNodeInfo node = findNode(info.getChild(i), name);
            if (node != null) return node;
        }
        if (!info.getActionList().contains(ACTION_CLICK)) return null;

        CharSequence contentDescription = info.getContentDescription();
        if (TextUtils.isEmpty(contentDescription)) return null;

        String description = contentDescription.toString();
        if (description.contains(name)) return info;
        return null;
    }
}
