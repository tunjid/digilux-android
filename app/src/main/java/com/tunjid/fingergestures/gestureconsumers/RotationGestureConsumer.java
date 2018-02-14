package com.tunjid.fingergestures.gestureconsumers;


import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tunjid.fingergestures.services.FingerGestureService.ANDROID_SYSTEM_UI_PACKAGE;

public class RotationGestureConsumer implements GestureConsumer {

    private static final String ROTATION_APPS = "rotation apps";
    private static final String IGNORED_APPS = "ignored apps";
    private static final int ENABLE_AUTO_ROTATION = 1;
    private static final int DISABLE_AUTO_ROTATION = 0;

    private final App app;
    private final List<String> packageNames;

    private static RotationGestureConsumer instance;

    {
        app = App.getInstance();
        packageNames = new ArrayList<>();
        resetPackageNames();

        Set<String> ignored = getIgnoredApps();
        ignored.add(ANDROID_SYSTEM_UI_PACKAGE);
        saveSet(ignored, IGNORED_APPS);
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
        if (!App.canWriteToSettings()) return;

        String packageName = event.getPackageName().toString();
        if (packageNames.isEmpty() || getIgnoredApps().contains(packageName)) return;

        setAutoRotateOn(getRotationApps().contains(packageName));
    }

    public boolean addRotationApp(String packageName) {
        Set<String> rotationApps = getRotationApps();
        if (rotationApps.size() > 2 && PurchasesManager.getInstance().isNotPremium()) return false;

        rotationApps.add(packageName);

        saveSet(rotationApps, ROTATION_APPS);
        resetPackageNames();

        return true;
    }

    public void removeRotationApp(String packageName) {
        Set<String> rotationApps = getRotationApps();
        rotationApps.remove(packageName);

        saveSet(rotationApps, ROTATION_APPS);
        resetPackageNames();
    }

    public List<String> getPackageNames() {
        return packageNames;
    }

    private Set<String> getRotationApps() {
        return new HashSet<>(app.getPreferences().getStringSet(ROTATION_APPS, new HashSet<>()));
    }

    private Set<String> getIgnoredApps() {
        return new HashSet<>(app.getPreferences().getStringSet(IGNORED_APPS, new HashSet<>()));
    }

    private void saveSet(Set<String> set, String preferencesName) {
        app.getPreferences().edit().putStringSet(preferencesName, set).apply();
    }

    private void resetPackageNames() {
        packageNames.clear();

        getRotationApps().stream().filter(packageName -> {
            ApplicationInfo info = null;
            try {info = app.getPackageManager().getApplicationInfo(packageName, 0);}
            catch (Exception e) {e.printStackTrace();}

            return info != null;
        }).sorted().forEach(packageNames::add);
    }

    private boolean isAutoRotateOn() {
        return Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, DISABLE_AUTO_ROTATION) == ENABLE_AUTO_ROTATION;
    }

    private void setAutoRotateOn(boolean isOn) {
        int enabled = isOn ? ENABLE_AUTO_ROTATION : DISABLE_AUTO_ROTATION;
        Settings.System.putInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled);
    }
}
