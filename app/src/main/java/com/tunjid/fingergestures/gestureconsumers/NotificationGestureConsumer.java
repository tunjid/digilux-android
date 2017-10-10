package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.widget.Toast;

import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_UP;

public class NotificationGestureConsumer implements GestureConsumer {

    private final Set<Integer> gestures;

    private static NotificationGestureConsumer instance;

    static NotificationGestureConsumer getInstance() {
        if (instance == null) instance = new NotificationGestureConsumer();
        return instance;
    }

    private NotificationGestureConsumer() {
        gestures = new HashSet<>();
        gestures.add(NOTIFICATION_UP);
        gestures.add(NOTIFICATION_DOWN);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureUtils.GestureAction int gestureAction) {
        switch (gestureAction) {
            case NOTIFICATION_UP:
                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                Application.getContext().sendBroadcast(closeIntent);
                break;
            case NOTIFICATION_DOWN:
                try {
                    @SuppressLint("WrongConstant")
                    Object sbservice = Application.getContext().getSystemService("statusbar");
                    Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                    Method showsb;
                    showsb = statusbarManager.getMethod("expandNotificationsPanel");
                    showsb.invoke(sbservice);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(Application.getContext(), R.string.unsupported_action, Toast.LENGTH_SHORT);
                }
                break;
        }
    }

    @Override
    public Set<Integer> gestures() {
        return gestures;
    }
}

