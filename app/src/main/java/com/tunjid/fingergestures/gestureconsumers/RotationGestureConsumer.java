package com.tunjid.fingergestures.gestureconsumers;


import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.support.annotation.StringDef;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.tunjid.fingergestures.services.FingerGestureService.ANDROID_SYSTEM_UI_PACKAGE;

public class RotationGestureConsumer implements GestureConsumer {

    private static final int ENABLE_AUTO_ROTATION = 1;
    private static final int DISABLE_AUTO_ROTATION = 0;

    public static final String ROTATION_APPS = "rotation apps";
    public static final String EXCLUDED_APPS = "excluded rotation apps";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ROTATION_APPS, EXCLUDED_APPS})
    public @interface PersistedSet {}


    private final App app;
    private final Map<String, List<String>> packageMap;

    private static RotationGestureConsumer instance;

    {
        app = App.getInstance();
        packageMap = new HashMap<>();

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
        return false;
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!App.canWriteToSettings() || event.getEventType() != TYPE_WINDOW_STATE_CHANGED) return;

        Set<String> rotationApps = getSet(ROTATION_APPS);
        String packageName = event.getPackageName().toString();

        if (rotationApps.isEmpty() || getSet(EXCLUDED_APPS).contains(packageName)) return;

        setAutoRotateOn(rotationApps.contains(packageName));
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

    private void resetPackageNames(@PersistedSet String preferencesName) {
        List<String> packageNames = packageMap.computeIfAbsent(preferencesName, k -> new ArrayList<>());
        packageNames.clear();

        getSet(preferencesName).stream().filter(packageName -> {
            ApplicationInfo info = null;
            try {info = app.getPackageManager().getApplicationInfo(packageName, 0);}
            catch (Exception e) {e.printStackTrace();}

            return info != null;
        }).sorted().forEach(packageNames::add);
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
