package com.tunjid.fingergestures;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.ads.MobileAds;
import com.tunjid.fingergestures.services.FingerGestureService;

import static android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES;

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

    public static boolean canWriteToSettings() {
        return Settings.System.canWrite(getInstance());
    }

    public static boolean isAccessibilityServiceEnabled() {
        Context context = getInstance();
        ContentResolver contentResolver = context.getContentResolver();
        ComponentName expectedComponentName = new ComponentName(context, FingerGestureService.class);
        String enabledServicesSetting = Settings.Secure.getString(contentResolver, ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServicesSetting == null) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName)) return true;
        }

        return false;
    }
}
