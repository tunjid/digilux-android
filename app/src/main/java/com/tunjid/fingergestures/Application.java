package com.tunjid.fingergestures;

import android.content.Context;

import com.google.android.gms.ads.MobileAds;

public class Application extends android.app.Application {

    private static Application context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        MobileAds.initialize(this, getString(R.string.ad_id));
    }

    public static Context getContext() {
        return context;
    }
}
