package com.tunjid.fingergestures.gestureconsumers;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.SetManager;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.tunjid.fingergestures.App.withApp;
import static com.tunjid.fingergestures.services.FingerGestureService.ANDROID_SYSTEM_UI_PACKAGE;

public class RotationGestureConsumer implements GestureConsumer {

    private static final int ENABLE_AUTO_ROTATION = 1;
    private static final int DISABLE_AUTO_ROTATION = 0;

    public static final String ACTION_WATCH_WINDOW_CHANGES = "com.tunjid.fingergestures.watch_windows";
    public static final String EXTRA_WATCHES_WINDOWS = "extra watches window content";
    public static final String EXCLUDED_APPS = "excluded rotation apps";
    public static final String ROTATION_APPS = "rotation apps";
    private static final String WATCHES_WINDOW_CONTENT = "watches window content";
    private static final String EMPTY_STRING = "";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ROTATION_APPS, EXCLUDED_APPS})
    public @interface PersistedSet {}

    private final SetManager<ApplicationInfo> setManager;

    private String lastPackageName;

    private static RotationGestureConsumer instance;

    private RotationGestureConsumer() {
        setManager = new SetManager<>(this::compareApplicationInfo, this::canAddToSet, this::fromPackageName, this::fromApplicationInfo);
        setManager.addToSet(ANDROID_SYSTEM_UI_PACKAGE, EXCLUDED_APPS);
        withApp(app -> setManager.addToSet(app.getPackageName(), EXCLUDED_APPS));
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
        if (event == null) return;
        if (!App.canWriteToSettings() || event.getEventType() != TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence sequence = event.getPackageName();
        if (sequence == null) return;

        String packageName = sequence.toString();
        if (packageName.equals(lastPackageName)) return;

        Set<String> rotationApps = setManager.getSet(ROTATION_APPS);
        if (rotationApps.isEmpty() || setManager.getSet(EXCLUDED_APPS).contains(packageName))
            return;

        lastPackageName = packageName;
        setAutoRotateOn(rotationApps.contains(packageName));
    }

    public String getAddText(@PersistedSet String preferencesName) {
        return App.transformApp(app -> app.getString(R.string.auto_rotate_add, ROTATION_APPS.equals(preferencesName)
                ? app.getString(R.string.auto_rotate_apps)
                : app.getString(R.string.auto_rotate_apps_excluded)), EMPTY_STRING);
    }

    public String getRemoveText(@PersistedSet String preferencesName) {
        return App.transformApp(app -> app.getString(R.string.auto_rotate_remove, ROTATION_APPS.equals(preferencesName)
                ? app.getString(R.string.auto_rotate_apps)
                : app.getString(R.string.auto_rotate_apps_excluded)), EMPTY_STRING);
    }

    public boolean isRemovable(String packageName) {
        return App.transformApp(app -> (!ANDROID_SYSTEM_UI_PACKAGE.equals(packageName) && !app.getPackageName().equals(packageName)), false);
    }

    public boolean addToSet(String packageName, @PersistedSet String preferencesName) {
        return setManager.addToSet(packageName, preferencesName);
    }

    public void removeFromSet(String packageName, @PersistedSet String preferencesName) {
        setManager.removeFromSet(packageName, preferencesName);
    }

    public List<ApplicationInfo> getList(@PersistedSet String preferenceName) {
        return setManager.getItems(preferenceName);
    }

    public Comparator<ApplicationInfo> getApplicationInfoComparator() {
        return this::compareApplicationInfo;
    }

    public boolean canAutoRotate() {
        return App.transformApp(app -> app.getPreferences().getBoolean(WATCHES_WINDOW_CONTENT, false), false);
    }

    public void enableWindowContentWatching(boolean enabled) {
        withApp(app -> {
            app.getPreferences().edit().putBoolean(WATCHES_WINDOW_CONTENT, enabled).apply();

            Intent intent = new Intent(ACTION_WATCH_WINDOW_CHANGES);
            intent.putExtra(EXTRA_WATCHES_WINDOWS, enabled);

            app.broadcast(intent);
        });
    }

    private void setAutoRotateOn(boolean isOn) {
        withApp(app -> {
            int enabled = isOn ? ENABLE_AUTO_ROTATION : DISABLE_AUTO_ROTATION;
            Settings.System.putInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled);
        });
    }

    private boolean isAutoRotateOn() {
        return App.transformApp(app -> Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, DISABLE_AUTO_ROTATION) == ENABLE_AUTO_ROTATION, false);
    }

    private boolean canAddToSet(String preferenceName) {
        Set<String> set = setManager.getSet(preferenceName);
        long count = set.stream().filter(this::isRemovable).count();
        return count < 2 || PurchasesManager.getInstance().isPremiumNotTrial();
    }

    private int compareApplicationInfo(ApplicationInfo infoA, ApplicationInfo infoB) {
        return App.transformApp(app -> {
            PackageManager packageManager = app.getPackageManager();
            return packageManager.getApplicationLabel(infoA).toString().compareTo(packageManager.getApplicationLabel(infoB).toString());
        }, 0);
    }

    private String fromApplicationInfo(ApplicationInfo info) {
        return info.packageName;
    }

    @Nullable
    private ApplicationInfo fromPackageName(String packageName) {
        return App.transformApp(app -> {
            try {return app.getPackageManager().getApplicationInfo(packageName, 0);}
            catch (Exception e) {e.printStackTrace();}

            return null;
        }, null);
    }
}
