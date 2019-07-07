/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.content.Intent;

import static com.tunjid.fingergestures.App.withApp;

public class NotificationGestureConsumer implements GestureConsumer {

    public static final String ACTION_NOTIFICATION_UP = "NotificationGestureConsumer up";
    public static final String ACTION_NOTIFICATION_DOWN = "NotificationGestureConsumer down";
    public static final String ACTION_NOTIFICATION_TOGGLE = "NotificationGestureConsumer toggle";

    private static NotificationGestureConsumer instance;

    static NotificationGestureConsumer getInstance() {
        if (instance == null) instance = new NotificationGestureConsumer();
        return instance;
    }

    private NotificationGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        switch (gesture) {
            case NOTIFICATION_UP:
            case NOTIFICATION_DOWN:
            case NOTIFICATION_TOGGLE:
                return true;
            default:
                return false;
        }
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case NOTIFICATION_UP:
            case NOTIFICATION_DOWN:
            case NOTIFICATION_TOGGLE:
                Companion.withApp(app -> app.broadcast(new Intent(gestureAction == NOTIFICATION_UP
                        ? ACTION_NOTIFICATION_UP
                        : gestureAction == NOTIFICATION_DOWN
                        ? ACTION_NOTIFICATION_DOWN
                        : ACTION_NOTIFICATION_TOGGLE)));
                break;
        }
    }
}

