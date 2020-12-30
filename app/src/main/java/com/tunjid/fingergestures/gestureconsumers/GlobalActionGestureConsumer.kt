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

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
import android.annotation.SuppressLint
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.models.Broadcast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalActionGestureConsumer @Inject constructor(
   private val broadcaster: AppBroadcaster
) : GestureConsumer {

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean = when (gesture) {
        GestureAction.GlobalHome,
        GestureAction.GlobalBack,
        GestureAction.GlobalRecents,
        GestureAction.GlobalLockScreen,
        GestureAction.GlobalSplitScreen,
        GestureAction.GlobalPowerDialog,
        GestureAction.GlobalTakeScreenshot -> true
        else -> false
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        var action = -1

        when (gestureAction) {
            GestureAction.GlobalHome -> action = GLOBAL_ACTION_HOME
            GestureAction.GlobalBack -> action = GLOBAL_ACTION_BACK
            GestureAction.GlobalRecents -> action = GLOBAL_ACTION_RECENTS
            GestureAction.GlobalSplitScreen -> action = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
            GestureAction.GlobalPowerDialog -> action = GLOBAL_ACTION_POWER_DIALOG
        }

        if (App.isPieOrHigher) {
            if (gestureAction == GestureAction.GlobalTakeScreenshot) action = GLOBAL_ACTION_TAKE_SCREENSHOT
            else if (gestureAction == GestureAction.GlobalLockScreen) action = GLOBAL_ACTION_LOCK_SCREEN
        }

        if (action == -1) return

        broadcaster(Broadcast.Service.GlobalAction(action))
    }
}

