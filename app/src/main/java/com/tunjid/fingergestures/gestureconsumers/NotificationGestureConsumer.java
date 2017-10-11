package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.tunjid.fingergestures.Application;

import java.util.HashSet;
import java.util.Set;

import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_DOWN;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NOTIFICATION_UP;

public class NotificationGestureConsumer implements GestureConsumer {

    public static final String ACTION_NOTIFICATION_UP = "NotificationGestureConsumer up";
    public static final String ACTION_NOTIFICATION_DOWN = "NotificationGestureConsumer down";

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
            case NOTIFICATION_DOWN:
                LocalBroadcastManager.getInstance(Application.getContext())
                        .sendBroadcast(new Intent(gestureAction == NOTIFICATION_UP
                                ? ACTION_NOTIFICATION_UP
                                : ACTION_NOTIFICATION_DOWN));
                break;
        }
    }

    @Override
    public Set<Integer> gestures() {
        return gestures;
    }
}

