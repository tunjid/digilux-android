package com.tunjid.fingergestures;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WallpaperBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BackgroundManager.getInstance().onIntentReceived(intent);
    }
}