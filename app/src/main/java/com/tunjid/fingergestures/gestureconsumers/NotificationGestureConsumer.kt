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

package com.tunjid.fingergestures.gestureconsumers

import android.annotation.SuppressLint
import android.content.Intent
import com.tunjid.fingergestures.App


class NotificationGestureConsumer private constructor() : GestureConsumer {

    @SuppressLint("SwitchIntDef")
    override fun accepts(@GestureConsumer.GestureAction gesture: Int): Boolean {
        return when (gesture) {
            GestureConsumer.NOTIFICATION_UP, GestureConsumer.NOTIFICATION_DOWN, GestureConsumer.NOTIFICATION_TOGGLE -> true
            else -> false
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(@GestureConsumer.GestureAction gestureAction: Int) {
        when (gestureAction) {
            GestureConsumer.NOTIFICATION_UP,
            GestureConsumer.NOTIFICATION_DOWN,
            GestureConsumer.NOTIFICATION_TOGGLE -> App.withApp { app ->
                app.broadcast(Intent(when (gestureAction) {
                    GestureConsumer.NOTIFICATION_UP -> ACTION_NOTIFICATION_UP
                    GestureConsumer.NOTIFICATION_DOWN -> ACTION_NOTIFICATION_DOWN
                    else -> ACTION_NOTIFICATION_TOGGLE
                }))
            }
        }
    }

    companion object {

        const val ACTION_NOTIFICATION_UP = "NotificationGestureConsumer up"
        const val ACTION_NOTIFICATION_DOWN = "NotificationGestureConsumer down"
        const val ACTION_NOTIFICATION_TOGGLE = "NotificationGestureConsumer toggle"

        val instance: NotificationGestureConsumer by lazy { NotificationGestureConsumer() }

    }
}

