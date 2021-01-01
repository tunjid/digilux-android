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
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.models.Broadcast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationGestureConsumer @Inject constructor(
    private val broadcaster: AppBroadcaster
) : GestureConsumer {

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean {
        return when (gesture) {
            GestureAction.NotificationUp,
            GestureAction.NotificationDown,
            GestureAction.NotificationToggle -> true
            else -> false
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        when (gestureAction) {
            GestureAction.NotificationUp,
            GestureAction.NotificationDown,
            GestureAction.NotificationToggle -> broadcaster(when (gestureAction) {
                GestureAction.NotificationUp -> Broadcast.Service.ShadeUp
                GestureAction.NotificationDown -> Broadcast.Service.ShadeDown
                else -> Broadcast.Service.ShadeToggle
            })
            else -> Unit
        }
    }
}

