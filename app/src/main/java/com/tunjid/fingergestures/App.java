package com.tunjid.fingergestures;

import android.content.SharedPreferences;

import com.google.android.gms.ads.MobileAds;

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
}
