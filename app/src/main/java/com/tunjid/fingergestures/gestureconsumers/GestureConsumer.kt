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
enum class GestureAction(val id: Int) {
    IncreaseBrightness(0),
    ReduceBrightness(1),
    MaximizeBrightness(2),
    MinimizeBrightness(3),
    // int NIGHT_MODE_ON = 4; DO NOT REMOVE FOR LEGACY REASONS
    // int NIGHT_MODE_OFF = 5; DO NOT REMOVE FOR LEGACY REASONS
    NotificationUp(6),
    NotificationDown(7),
    DoNothing(8),
    FlashlightToggle(9),
    DockToggle(10),
    AutoRotateToggle(11),
    NotificationToggle(12),
    GlobalHome(13),
    GlobalBack(14),
    GlobalRecents(15),
    GlobalSplitScreen(16),
    GlobalPowerDialog(17),
    PopUpShow(18),
    IncreaseAudio(19),
    ReduceAudio(20),
    GlobalLockScreen(21),
    GlobalTakeScreenshot(22);

    val serialized get() = id.toString()

    companion object {
        private val map = values().map { it.id to it }.toMap()
        fun fromId(id: Int) = map.getValue(id)
        fun deserialize(id: String) = fromId(id.toInt())
    }
}

val GestureAction.resource: Int
    get() = when (this) {
        GestureAction.DoNothing -> R.string.do_nothing
        GestureAction.IncreaseBrightness -> R.string.increase_brightness
        GestureAction.ReduceBrightness -> R.string.reduce_brightness
        GestureAction.MaximizeBrightness -> R.string.maximize_brightness
        GestureAction.MinimizeBrightness -> R.string.minimize_brightness
        GestureAction.IncreaseAudio -> R.string.increase_audio
        GestureAction.ReduceAudio -> R.string.reduce_audio
        GestureAction.NotificationUp -> R.string.notification_up
        GestureAction.NotificationDown -> R.string.notification_down
        GestureAction.NotificationToggle -> R.string.toggle_notifications
        GestureAction.FlashlightToggle -> R.string.toggle_flashlight
        GestureAction.DockToggle -> R.string.toggle_dock
        GestureAction.AutoRotateToggle -> R.string.toggle_auto_rotate
        GestureAction.GlobalHome -> R.string.global_home
        GestureAction.GlobalBack -> R.string.global_back
        GestureAction.GlobalRecents -> R.string.global_recents
        GestureAction.GlobalSplitScreen -> R.string.global_split_screen
        GestureAction.GlobalPowerDialog -> R.string.global_power_dialog
        GestureAction.GlobalLockScreen -> R.string.global_lock_screen
        GestureAction.GlobalTakeScreenshot -> R.string.global_take_screenshot
        GestureAction.PopUpShow -> R.string.show_popup
    }

interface GestureConsumer {

    fun onGestureActionTriggered(gestureAction: GestureAction)

    fun accepts(gesture: GestureAction): Boolean

    companion object {

        const val ZERO_PERCENT = 0
        const val FIFTY_PERCENT = 50
        const val HUNDRED_PERCENT = 100

        fun normalizePercentageToFraction(percentage: Int): Float = percentage / 100f
    }
}
