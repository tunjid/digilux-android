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

import androidx.annotation.IntDef

interface GestureConsumer {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
            INCREASE_BRIGHTNESS,
            REDUCE_BRIGHTNESS,
            MAXIMIZE_BRIGHTNESS,
            MINIMIZE_BRIGHTNESS,
            NOTIFICATION_DOWN,
            NOTIFICATION_UP,
            NOTIFICATION_TOGGLE,
            DO_NOTHING,
            TOGGLE_FLASHLIGHT,
            TOGGLE_DOCK,
            TOGGLE_AUTO_ROTATE,
            GLOBAL_HOME,
            GLOBAL_BACK,
            GLOBAL_RECENTS,
            GLOBAL_SPLIT_SCREEN,
            GLOBAL_POWER_DIALOG,
            SHOW_POPUP,
            REDUCE_AUDIO,
            INCREASE_AUDIO,
            GLOBAL_LOCK_SCREEN,
            GLOBAL_TAKE_SCREENSHOT
    )
    annotation class GestureAction

    fun onGestureActionTriggered(@GestureAction gestureAction: Int)

    fun accepts(@GestureAction gesture: Int): Boolean

    companion object {

        const val ZERO_PERCENT = 0
        const val FIFTY_PERCENT = 50
        const val HUNDRED_PERCENT = 100

        const val INCREASE_BRIGHTNESS = 0
        const val REDUCE_BRIGHTNESS = 1
        const val MAXIMIZE_BRIGHTNESS = 2
        const val MINIMIZE_BRIGHTNESS = 3
        // int NIGHT_MODE_ON = 4; DO NOT REMOVE FOR LEGACY REASONS
        // int NIGHT_MODE_OFF = 5; DO NOT REMOVE FOR LEGACY REASONS
        const val NOTIFICATION_UP = 6
        const val NOTIFICATION_DOWN = 7
        const val DO_NOTHING = 8
        const val TOGGLE_FLASHLIGHT = 9
        const val TOGGLE_DOCK = 10
        const val TOGGLE_AUTO_ROTATE = 11
        const val NOTIFICATION_TOGGLE = 12
        const val GLOBAL_HOME = 13
        const val GLOBAL_BACK = 14
        const val GLOBAL_RECENTS = 15
        const val GLOBAL_SPLIT_SCREEN = 16
        const val GLOBAL_POWER_DIALOG = 17
        const val SHOW_POPUP = 18
        const val INCREASE_AUDIO = 19
        const val REDUCE_AUDIO = 20
        const val GLOBAL_LOCK_SCREEN = 21
        const val GLOBAL_TAKE_SCREENSHOT = 22

        fun normalizePercentageToFraction(percentage: Int): Float {
            return percentage / 100f
        }
    }

}
