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
import android.content.Intent
import com.tunjid.fingergestures.App

class GlobalActionGestureConsumer private constructor() : GestureConsumer {

    @SuppressLint("SwitchIntDef")
    override fun accepts(@GestureConsumer.GestureAction gesture: Int): Boolean = when (gesture) {
        GestureConsumer.GLOBAL_HOME,
        GestureConsumer.GLOBAL_BACK,
        GestureConsumer.GLOBAL_RECENTS,
        GestureConsumer.GLOBAL_LOCK_SCREEN,
        GestureConsumer.GLOBAL_SPLIT_SCREEN,
        GestureConsumer.GLOBAL_POWER_DIALOG,
        GestureConsumer.GLOBAL_TAKE_SCREENSHOT -> true
        else -> false
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(@GestureConsumer.GestureAction gestureAction: Int) {
        val app = App.instance ?: return

        var action = -1

        when (gestureAction) {
            GestureConsumer.GLOBAL_HOME -> action = GLOBAL_ACTION_HOME
            GestureConsumer.GLOBAL_BACK -> action = GLOBAL_ACTION_BACK
            GestureConsumer.GLOBAL_RECENTS -> action = GLOBAL_ACTION_RECENTS
            GestureConsumer.GLOBAL_SPLIT_SCREEN -> action = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
            GestureConsumer.GLOBAL_POWER_DIALOG -> action = GLOBAL_ACTION_POWER_DIALOG
        }

        if (App.isPieOrHigher) {
            if (gestureAction == GestureConsumer.GLOBAL_TAKE_SCREENSHOT) action = GLOBAL_ACTION_TAKE_SCREENSHOT
            else if (gestureAction == GestureConsumer.GLOBAL_LOCK_SCREEN) action = GLOBAL_ACTION_LOCK_SCREEN
        }

        if (action == -1) return

        val intent = Intent(ACTION_GLOBAL_ACTION)
        intent.putExtra(EXTRA_GLOBAL_ACTION, action)

        app.broadcast(intent)
    }

    companion object {

        const val ACTION_GLOBAL_ACTION = "GlobalActionConsumer action"
        const val EXTRA_GLOBAL_ACTION = "GlobalActionConsumer action extra"

        val instance: GlobalActionGestureConsumer by lazy { GlobalActionGestureConsumer() }

    }
}

