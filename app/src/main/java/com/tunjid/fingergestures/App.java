package com.tunjid.fingergestures;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.google.android.gms.ads.MobileAds;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static io.reactivex.Flowable.timer;

public class App extends android.app.Application {

    private static final String BRIGHTNESS_PREFS = "brightness prefs";

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MobileAds.initialize(this, getString(R.string.ad_id));
    }

    public SharedPreferences getPreferences() {
        return getSharedPreferences(BRIGHTNESS_PREFS, MODE_PRIVATE);
    }

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

    public static Disposable delay(long interval, TimeUnit timeUnit, Runnable runnable) {
        return timer(interval, timeUnit).subscribe(ignored -> runnable.run(), Throwable::printStackTrace);
    }

    public static boolean canWriteToSettings() {
        return Settings.System.canWrite(getInstance());
    }

    public static boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(getInstance(), READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }

    public static boolean accessibilityServiceEnabled() {
        App app = getInstance();
        String packageName = app.getPackageName();

        AccessibilityManager accessibilityManager = ((AccessibilityManager) app.getSystemService(Context.ACCESSIBILITY_SERVICE));
        if (accessibilityManager == null) return false;

        List<AccessibilityServiceInfo> sevices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);

        for (AccessibilityServiceInfo info : sevices) if (info.getId().contains(packageName)) return true;
        return false;
    }
}
