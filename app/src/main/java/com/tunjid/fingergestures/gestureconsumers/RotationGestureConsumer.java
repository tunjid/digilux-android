package com.tunjid.fingergestures.gestureconsumers;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.annotation.StringDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.tunjid.fingergestures.services.FingerGestureService.ANDROID_SYSTEM_UI_PACKAGE;

public class RotationGestureConsumer implements GestureConsumer {

    private static final int ENABLE_AUTO_ROTATION = 1;
    private static final int DISABLE_AUTO_ROTATION = 0;

    public static final String ACTION_WATCH_WINDOW_CHANGES = "com.tunjid.fingergestures.watch_windows";
    public static final String EXTRA_WATCHES_WINDOWS = "extra watches window content";
    public static final String EXCLUDED_APPS = "excluded rotation apps";
    public static final String ROTATION_APPS = "rotation apps";
    private static final String WATCHES_WINDOW_CONTENT = "watches window content";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ROTATION_APPS, EXCLUDED_APPS})
    public @interface PersistedSet {}

    private final App app;
    private final Map<String, List<String>> packageMap;
    private final Comparator<ApplicationInfo> applicationInfoComparator;

    private String lastPackageName;

    private static RotationGestureConsumer instance;

    {
        app = App.getInstance();
        packageMap = new HashMap<>();
        applicationInfoComparator = (infoA, infoB) -> {
            PackageManager packageManager = app.getPackageManager();
            return packageManager.getApplicationLabel(infoA).toString().compareTo(packageManager.getApplicationLabel(infoB).toString());
        };

        Set<String> ignored = getSet(EXCLUDED_APPS);
        ignored.add(app.getPackageName());
        ignored.add(ANDROID_SYSTEM_UI_PACKAGE);

        saveSet(ignored, EXCLUDED_APPS);
        resetPackageNames(ROTATION_APPS);
        resetPackageNames(EXCLUDED_APPS);
    }

    public static RotationGestureConsumer getInstance() {
        if (instance == null) instance = new RotationGestureConsumer();
        return instance;
    }

    @Override
    public void onGestureActionTriggered(int gestureAction) {
        setAutoRotateOn(!isAutoRotateOn());
    }

    @Override
    public boolean accepts(int gesture) {
        return gesture == TOGGLE_AUTO_ROTATE;
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!App.canWriteToSettings() || event.getEventType() != TYPE_WINDOW_STATE_CHANGED) return;

        String packageName = event.getPackageName().toString();
        if (packageName.equals(lastPackageName)) return;

        Set<String> rotationApps = getSet(ROTATION_APPS);
        if (rotationApps.isEmpty() || getSet(EXCLUDED_APPS).contains(packageName)) return;

        lastPackageName = packageName;
        setAutoRotateOn(rotationApps.contains(packageName));
        Log.i("TEST", "Accessibility event: " + packageName);
    }

    public String getAddText(@PersistedSet String preferencesName) {
        return app.getString(R.string.auto_rotate_add, ROTATION_APPS.equals(preferencesName)
                ? app.getString(R.string.auto_rotate_apps)
                : app.getString(R.string.auto_rotate_apps_excluded));
    }

    public String getRemoveText(@PersistedSet String preferencesName) {
        return app.getString(R.string.auto_rotate_remove, ROTATION_APPS.equals(preferencesName)
                ? app.getString(R.string.auto_rotate_apps)
                : app.getString(R.string.auto_rotate_apps_excluded));
    }

    public List<String> getList(@PersistedSet String preferenceName) {
        return packageMap.get(preferenceName);
    }

    public boolean isRemovable(String packageName) {
        return !ANDROID_SYSTEM_UI_PACKAGE.equals(packageName) && !app.getPackageName().equals(packageName);
    }

    public boolean addToSet(String packageName, @PersistedSet String preferencesName) {
        Set<String> set = getSet(preferencesName);
        if (set.size() > 2 && PurchasesManager.getInstance().isNotPremium()) return false;

        set.add(packageName);

        saveSet(set, preferencesName);
        resetPackageNames(preferencesName);

        return true;
    }

    public void removeFromSet(String packageName, @PersistedSet String preferencesName) {
        Set<String> set = getSet(preferencesName);
        set.remove(packageName);

        saveSet(set, preferencesName);
        resetPackageNames(preferencesName);
    }

    public Comparator<ApplicationInfo> getApplicationInfoComparator() {
        return applicationInfoComparator;
    }

    public boolean canAutoRotate() {
        return app.getPreferences().getBoolean(WATCHES_WINDOW_CONTENT, false);
    }

    public void enableWindowContentWatching(boolean enabled) {
        app.getPreferences().edit().putBoolean(WATCHES_WINDOW_CONTENT, enabled).apply();

        Intent intent = new Intent(ACTION_WATCH_WINDOW_CHANGES);
        intent.putExtra(EXTRA_WATCHES_WINDOWS, enabled);

        LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
    }

    private void resetPackageNames(@PersistedSet String preferencesName) {
        List<String> packageNames = packageMap.computeIfAbsent(preferencesName, k -> new ArrayList<>());
        packageNames.clear();

        getSet(preferencesName).stream()
                .map(packageName -> {
                    ApplicationInfo info = null;
                    try {info = app.getPackageManager().getApplicationInfo(packageName, 0);}
                    catch (Exception e) {e.printStackTrace();}

                    return info;
                })
                .filter(Objects::nonNull)
                .sorted(applicationInfoComparator).forEach(applicationInfo -> packageNames.add(applicationInfo.packageName));
    }

    private Set<String> getSet(@PersistedSet String preferencesName) {
        return new HashSet<>(app.getPreferences().getStringSet(preferencesName, new HashSet<>()));
    }

    private void saveSet(Set<String> set, @PersistedSet String preferencesName) {
        app.getPreferences().edit().putStringSet(preferencesName, set).apply();
    }

    private boolean isAutoRotateOn() {
        return Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, DISABLE_AUTO_ROTATION) == ENABLE_AUTO_ROTATION;
    }

    private void setAutoRotateOn(boolean isOn) {
        int enabled = isOn ? ENABLE_AUTO_ROTATION : DISABLE_AUTO_ROTATION;
        Settings.System.putInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled);
    }
}
