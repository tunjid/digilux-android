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

import com.tunjid.fingergestures.R

/**
 * Actions available. The ids are serialized to shared preferences and MUST not change
 */

enum class GestureAction(val id: Int, val nameRes: Int) {
    IncreaseBrightness(id = 0, nameRes = R.string.increase_brightness),
    ReduceBrightness(id = 1, nameRes = R.string.reduce_brightness),
    MaximizeBrightness(id = 2, nameRes = R.string.maximize_brightness),
    MinimizeBrightness(id = 3, nameRes = R.string.minimize_brightness),
    // int NIGHT_MODE_ON = 4; DO NOT REMOVE FOR LEGACY REASONS
    // int NIGHT_MODE_OFF = 5; DO NOT REMOVE FOR LEGACY REASONS
    NotificationUp(id = 6, nameRes = R.string.notification_up),
    NotificationDown(id = 7, nameRes = R.string.notification_down),
    DoNothing(id = 8, nameRes = R.string.do_nothing),
    FlashlightToggle(id = 9, nameRes = R.string.toggle_flashlight),
    DockToggle(id = 10, nameRes = R.string.toggle_dock),
    AutoRotateToggle(id = 11, nameRes = R.string.toggle_auto_rotate),
    NotificationToggle(id = 12, nameRes = R.string.toggle_notifications),
    GlobalHome(id = 13, nameRes = R.string.global_home),
    GlobalBack(id = 14, nameRes = R.string.global_back),
    GlobalRecents(id = 15, nameRes = R.string.global_recents),
    GlobalSplitScreen(id = 16, nameRes = R.string.global_split_screen),
    GlobalPowerDialog(id = 17, nameRes = R.string.global_power_dialog),
    PopUpShow(id = 18, nameRes = R.string.show_popup),
    IncreaseAudio(id = 19, nameRes = R.string.increase_audio),
    ReduceAudio(id = 20, nameRes = R.string.reduce_audio),
    GlobalLockScreen(id = 21, nameRes = R.string.global_lock_screen),
    GlobalTakeScreenshot(id = 22, nameRes = R.string.global_take_screenshot);

    val serialized get() = id.toString()

    companion object {
        private val map = values().map { it.id to it }.toMap()
        fun fromId(id: Int) = map.getValue(id)
        fun deserialize(id: String) = fromId(id.toInt())
    }
}

interface GestureConsumer {

    fun onGestureActionTriggered(gestureAction: GestureAction)

    fun accepts(gesture: GestureAction): Boolean

    companion object {
        const val ZERO_PERCENT = 0
        const val HUNDRED_PERCENT = 100
        fun normalizePercentageToFraction(percentage: Int): Float = percentage / 100f
    }
}
