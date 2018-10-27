package com.tunjid.fingergestures;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.accessibility.AccessibilityManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import io.reactivex.disposables.Disposable;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.accessibility.AccessibilityEvent.TYPES_ALL_MASK;
import static io.reactivex.Flowable.timer;

public class App extends android.app.Application {

    private static final String BRIGHTNESS_PREFS = "brightness prefs";
    public static final String EMPTY = "";

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public SharedPreferences getPreferences() {
        return getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
    }

    @Nullable
    public static App getInstance() {
        return instance;
    }

    @NonNull
    public static Intent settingsIntent() {
        return new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getInstance().getPackageName()));
    }

    @NonNull
    public static Intent accessibilityIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    @NonNull
    public static Intent doNotDisturbIntent() {
        return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
    }

    public static Disposable delay(long interval, TimeUnit timeUnit, Runnable runnable) {
        return timer(interval, timeUnit).subscribe(ignored -> runnable.run(), Throwable::printStackTrace);
    }

    public static boolean canWriteToSettings() {
        return transformApp(Settings.System::canWrite, false);
    }

    public static boolean hasStoragePermission() {
        return transformApp(app -> ContextCompat.checkSelfPermission(app, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED, false);
    }

    public static boolean hasDoNotDisturbAccess() {
        return transformApp(app -> {
            NotificationManager notificationManager = app.getSystemService(NotificationManager.class);
            return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        }, false);
    }

    public static boolean accessibilityServiceEnabled() {
        return transformApp(app -> {
            String key = app.getPackageName();

            AccessibilityManager accessibilityManager = ((AccessibilityManager) app.getSystemService(ACCESSIBILITY_SERVICE));
            if (accessibilityManager == null) return false;

            List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(TYPES_ALL_MASK);

            for (AccessibilityServiceInfo info : list) if (info.getId().contains(key)) return true;
            return false;
        }, false);
    }

    public static void withApp(Consumer<App> appConsumer) {
        App app = getInstance();
        if (app == null) return;

        appConsumer.accept(app);
    }

    public static <T> T transformApp(Function<App, T> appTFunction, T defaultValue) {
        App app = getInstance();
        return app != null ? appTFunction.apply(app) : defaultValue;
    }

    @Nullable
    public static <T> T transformApp(Function<App, T> appTFunction) {
        return transformApp(appTFunction, null);
    }
}
